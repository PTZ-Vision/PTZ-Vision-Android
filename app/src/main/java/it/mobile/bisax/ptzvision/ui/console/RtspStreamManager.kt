package it.mobile.bisax.ptzvision.ui.console

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.SurfaceView
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.rtsp.RtspMediaSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Low-latency RTSP stream manager for PTZ-Vision.
 *
 * Techniques borrowed from iEvgeny/cctv-viewer (via qmlav / FFmpeg options):
 *
 *  1. **Minimal probe/analysis window** - qmlav uses probesize=500 KB and
 *     analyzeduration=0 us so FFmpeg starts decoding on the very first IDR
 *     frame instead of buffering several seconds of data first.
 *     We mirror this by setting ExoPlayer's min/max buffer to 150 ms and
 *     disabling the "start buffer" gate to the same 150 ms.
 *
 *  2. **Force RTP-over-TCP** - avoids NAT/UDP hole-punching delays and the
 *     built-in ExoPlayer fallback timer (default 4 s). Connection starts
 *     faster and more reliably on typical PTZ camera LAN setups.
 *
 *  3. **Short RTSP session timeout** - 3 s instead of the default ~30 s so
 *     the player detects a dead stream and reconnects quickly.
 *
 *  4. **Exponential-backoff auto-reconnect** - mirrors the reconnect logic in
 *     cctv-viewer (it re-opens the AVFormatContext on timeout); here we detect
 *     STATE_IDLE after an error and retry with doubling delay.
 */
@OptIn(UnstableApi::class)
class RtspStreamManager(private val context: Context) {

    companion object {
        private const val TAG = "RtspStreamManager"

        // Buffer tuning (mirrors qmlav analyzeduration ~ 0).
        private const val MIN_BUFFER_MS = 150
        private const val MAX_BUFFER_MS = 500
        private const val BUFFER_FOR_PLAYBACK_MS = 150
        private const val BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS = 300

        // RTSP session.
        private const val RTSP_TIMEOUT_MS = 3_000L   // fast dead-stream detection
        private const val RECONNECT_DELAY_MIN = 1_000L
        private const val RECONNECT_DELAY_MAX = 16_000L
    }

    sealed class StreamState {
        object Idle : StreamState()
        object Connecting : StreamState()
        object Playing : StreamState()
        data class Error(val cause: PlaybackException) : StreamState()
    }

    private val _state = MutableStateFlow<StreamState>(StreamState.Idle)
    val state: StateFlow<StreamState> = _state

    private var player: ExoPlayer? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var reconnectDelay = RECONNECT_DELAY_MIN
    private var currentUrl: String? = null

    fun start(rtspUrl: String, surface: SurfaceView) {
        currentUrl = rtspUrl
        reconnectDelay = RECONNECT_DELAY_MIN
        buildAndPlay(rtspUrl, surface)
    }

    fun stop() {
        mainHandler.removeCallbacksAndMessages(null)
        player?.release()
        player = null
        _state.value = StreamState.Idle
    }

    private fun buildAndPlay(url: String, surface: SurfaceView) {
        player?.release()

        val loadControl = DefaultLoadControl.Builder()
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
            .also { p ->
                p.setVideoSurfaceView(surface)
                p.volume = 0f
                p.playWhenReady = true
                p.addListener(playerListener(surface))
            }

        val mediaSource = RtspMediaSource.Factory()
            .setForceUseRtpTcp(true)
            .setTimeoutMs(RTSP_TIMEOUT_MS)
            .createMediaSource(MediaItem.fromUri(url))

        exoPlayer.setMediaSource(mediaSource)
        exoPlayer.prepare()

        player = exoPlayer
        _state.value = StreamState.Connecting
        Log.d(TAG, "Connecting to $url [minBuf=${MIN_BUFFER_MS}ms, tcp=true]")
    }

    private fun playerListener(surface: SurfaceView) = object : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            when (state) {
                Player.STATE_READY -> {
                    reconnectDelay = RECONNECT_DELAY_MIN
                    _state.value = StreamState.Playing
                    Log.d(TAG, "Playing")
                }
                Player.STATE_BUFFERING -> Log.d(TAG, "Buffering")
                Player.STATE_ENDED -> scheduleReconnect(surface)
                Player.STATE_IDLE -> { /* handled by onPlayerError */ }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            Log.w(TAG, "Player error: ${error.message}")
            _state.value = StreamState.Error(error)
            scheduleReconnect(surface)
        }
    }

    private fun scheduleReconnect(surface: SurfaceView) {
        val url = currentUrl ?: return
        Log.d(TAG, "Reconnecting in ${reconnectDelay}ms...")
        mainHandler.postDelayed({
            buildAndPlay(url, surface)
        }, reconnectDelay)
        reconnectDelay = (reconnectDelay * 2).coerceAtMost(RECONNECT_DELAY_MAX)
    }
}
