package it.mobile.bisax.ptzvision.ndi

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.isActive
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

/**
 * Discovers NDI® sources on the local network.
 *
 * - With the NDI runtime: the SDK finder (mDNS + optional extra IPs / discovery server config).
 * - Without it: plain mDNS browsing of `_ndi._tcp` via [NsdManager], so the user still sees
 *   which NDI devices exist (and their IP, usable e.g. for VISCA-over-IP).
 */
class NdiDiscovery(context: Context) {
    private val appContext = context.applicationContext

    /** True when results come from the NDI SDK (PTZ capable) rather than the mDNS fallback. */
    val usesNdiRuntime: Boolean get() = NdiRuntime.init(appContext)

    /**
     * Continuously emits the current list of sources until the collector is cancelled.
     * @param extraIps comma separated IPs to query directly (sources outside the mDNS domain).
     */
    fun sources(extraIps: String? = null): Flow<List<NdiSource>> {
        val base = if (usesNdiRuntime) sdkSources(extraIps) else mdnsSources()
        return base.withMulticastLock().distinctUntilChanged()
    }

    private fun sdkSources(extraIps: String?): Flow<List<NdiSource>> = flow {
        val finder = NdiNative.finderCreate(extraIps?.trim()?.ifEmpty { null })
        if (finder == 0L) error("NDI finder could not be created")
        try {
            emit(emptyList())
            while (currentCoroutineContext().isActive) {
                emit(decodeSources(NdiNative.finderGetSources(finder, 1000)))
            }
        } finally {
            NdiNative.finderDestroy(finder)
        }
    }.flowOn(Dispatchers.IO)

    private fun mdnsSources(): Flow<List<NdiSource>> = callbackFlow {
        val nsd = NdiRuntime.nsd() ?: error("NsdManager unavailable")
        val found = ConcurrentHashMap<String, NdiSource>()
        // NsdManager can only resolve one service at a time on older releases: serialise.
        val resolver = Executors.newSingleThreadExecutor()

        fun publish() {
            trySend(found.values.sortedBy { it.fullName.lowercase() })
        }

        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) = publish()
            override fun onDiscoveryStopped(serviceType: String) = Unit
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.w(TAG, "mDNS discovery failed: $errorCode")
                close(IllegalStateException("mDNS discovery failed ($errorCode)"))
            }
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) = Unit

            override fun onServiceFound(info: NsdServiceInfo) {
                val name = info.serviceName
                found.putIfAbsent(name, NdiSource(name))
                publish()
                resolver.execute { resolve(nsd, info) { address -> found[name] = NdiSource(name, address); publish() } }
            }

            override fun onServiceLost(info: NsdServiceInfo) {
                found.remove(info.serviceName)
                publish()
            }
        }

        nsd.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)
        awaitClose {
            runCatching { nsd.stopServiceDiscovery(listener) }
            resolver.shutdownNow()
        }
    }

    @Suppress("DEPRECATION") // resolveService is deprecated on API 34 but still the simplest portable API.
    private fun resolve(nsd: NsdManager, info: NsdServiceInfo, onResolved: (String) -> Unit) {
        val done = java.util.concurrent.CountDownLatch(1)
        nsd.resolveService(info, object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                done.countDown()
            }

            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                serviceInfo.host?.hostAddress?.let { onResolved("$it:${serviceInfo.port}") }
                done.countDown()
            }
        })
        done.await(3, java.util.concurrent.TimeUnit.SECONDS)
    }

    /** Some Wi-Fi drivers drop multicast (mDNS) traffic unless a multicast lock is held. */
    private fun <T> Flow<T>.withMulticastLock(): Flow<T> {
        val wifi = appContext.getSystemService(Context.WIFI_SERVICE) as WifiManager?
        val lock = wifi?.createMulticastLock("ptzvision-ndi-discovery")?.apply { setReferenceCounted(false) }
        return onStart { runCatching { lock?.acquire() } }
            .onCompletion { runCatching { lock?.release() } }
    }

    companion object {
        private const val TAG = "NdiDiscovery"
        const val SERVICE_TYPE = "_ndi._tcp"
    }
}
