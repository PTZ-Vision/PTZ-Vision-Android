package it.mobile.bisax.ptzvision.ui.console

import android.content.Context
import android.util.Log
import android.view.SurfaceView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.rtsp.RtspMediaSource
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import javax.net.SocketFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@UnstableApi
class RtspStreamManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onPlaying: () -> Unit,
    private val onError: (Throwable) -> Unit
) {
    private var player: ExoPlayer? = null
    private var reconnectJob: Job? = null
    private var currentUrl: String? = null
    private var currentSurfaceView: SurfaceView? = null
    private var backoffMs = INITIAL_BACKOFF_MS
    private var released = false

    fun start(rtspUrl: String, surfaceView: SurfaceView) {
        released = false
        currentUrl = rtspUrl
        currentSurfaceView = surfaceView
        backoffMs = INITIAL_BACKOFF_MS
        reconnectJob?.cancel()
        createPlayer(rtspUrl, surfaceView)
    }

    fun stop() {
        reconnectJob?.cancel()
        reconnectJob = null
        player?.release()
        player = null
    }

    fun release() {
        released = true
        stop()
    }

    private fun createPlayer(rtspUrl: String, surfaceView: SurfaceView) {
        player?.release()

        val loadControl = DefaultLoadControl
            .Builder()
            .setBufferDurationsMs(
                MIN_BUFFER_MS,
                MAX_BUFFER_MS,
                BUFFER_FOR_PLAYBACK_MS,
                BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val exoPlayer = ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
            .build()

        exoPlayer.setVideoSurfaceView(surfaceView)
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                scheduleReconnect(error)
            }

            override fun onRenderedFirstFrame() {
                backoffMs = INITIAL_BACKOFF_MS
                Log.d(TAG, "▶ Playing")
                onPlaying()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    scheduleReconnect(IllegalStateException("RTSP stream ended"))
                }
            }
        })

        val mediaItem = MediaItem.fromUri(rtspUrl)
        val rtspMediaSource = RtspMediaSource.Factory()
            .setForceUseRtpTcp(true)
            .setTimeoutMs(CONNECTION_TIMEOUT_MS)
            .setSocketFactory(TimeoutSocketFactory(CONNECTION_TIMEOUT_MS))
            .createMediaSource(mediaItem)

        exoPlayer.setMediaSource(rtspMediaSource)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true

        player = exoPlayer
    }

    private fun scheduleReconnect(error: Throwable) {
        if (released) {
            return
        }
        onError(error)
        reconnectJob?.cancel()
        val delayMs = backoffMs
        backoffMs = (backoffMs * 2).coerceAtMost(MAX_BACKOFF_MS)
        reconnectJob = scope.launch {
            delay(delayMs)
            if (released) return@launch
            val url = currentUrl
            val surface = currentSurfaceView
            if (url != null && surface != null) {
                createPlayer(url, surface)
            }
        }
    }

    private class TimeoutSocketFactory(
        private val timeoutMs: Int
    ) : SocketFactory() {
        private val delegate = getDefault()

        override fun createSocket(host: String?, port: Int): Socket {
            val socket = delegate.createSocket()
            socket.connect(
                InetSocketAddress(InetAddress.getByName(host), port),
                timeoutMs
            )
            return socket
        }

        override fun createSocket(
            host: String?,
            port: Int,
            localHost: InetAddress?,
            localPort: Int
        ): Socket {
            throw UnsupportedOperationException()
        }

        override fun createSocket(host: InetAddress?, port: Int): Socket {
            throw UnsupportedOperationException()
        }

        override fun createSocket(
            host: InetAddress?,
            port: Int,
            localHost: InetAddress?,
            localPort: Int
        ): Socket {
            throw UnsupportedOperationException()
        }
    }

    private companion object {
        const val TAG = "RtspStreamManager"
        const val MIN_BUFFER_MS = 150
        const val MAX_BUFFER_MS = 500
        const val BUFFER_FOR_PLAYBACK_MS = 150
        const val BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS = 300
        const val CONNECTION_TIMEOUT_MS = 3000
        const val INITIAL_BACKOFF_MS = 500L
        const val MAX_BACKOFF_MS = 8000L
    }
}
