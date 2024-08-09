package it.mobile.bisax.ptzvision.ui.console.blocks

import android.content.Context
import android.util.Log
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.rtsp.RtspMediaSource
import androidx.media3.ui.PlayerView
import it.mobile.bisax.ptzvision.data.cam.Cam
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import javax.net.SocketFactory

@OptIn(UnstableApi::class)
@Composable
fun SelectedCam(
    modifier: Modifier = Modifier,
    context: Context,
    cam: Cam? = null
) {
    var player: ExoPlayer? by remember { mutableStateOf(null) }
    var streamingError by remember(cam) { mutableStateOf(false) }

    fun resetPlayer() {
        player?.stop()
        player?.release()
        player = null
        streamingError = false
    }

    LaunchedEffect(cam) {
        resetPlayer()
        if (cam != null) {
            val loadControl = DefaultLoadControl
                .Builder()
                .setBufferDurationsMs(
                    500,  // minBufferMs - Set low to reduce latency
                    1000, // maxBufferMs - Set low to reduce latency
                    250,  // bufferForPlaybackMs - Quick start
                    500   // bufferForPlaybackAfterRebufferMs - Quick recovery
                )
                .setPrioritizeTimeOverSizeThresholds(true)
                .build()

            player = ExoPlayer.Builder(context).setLoadControl(loadControl).build()
            player?.addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    resetPlayer()
                    streamingError = true
                }

                override fun onRenderedFirstFrame() {
                    streamingError = false
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        resetPlayer()
                        streamingError = true
                    }
                }
            })
            val mediaItem = MediaItem.fromUri("rtsp://${cam.ip}:${cam.streamPort}/live")
            try {
                val rtspMediaSource = RtspMediaSource
                    .Factory()
                    .setForceUseRtpTcp(true)
                    .setSocketFactory(object : SocketFactory() {
                        private val defaultSocketFactory = getDefault()
                        override fun createSocket(host: String?, port: Int): Socket {
                            val socket = defaultSocketFactory.createSocket()
                            socket.connect(
                                InetSocketAddress(InetAddress.getByName(host), port),
                                1000
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
                    })
                    .createMediaSource(mediaItem)
                player?.addMediaSource(rtspMediaSource)
                player?.prepare()
                player?.play()
            } catch (e: Exception) {
                resetPlayer()
                Log.d("SelectedCam", "Error: ${e.message}")
                streamingError = true
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            resetPlayer()
        }
    }

    Box(
        modifier = modifier
            .background(Color(0xFF666666))
    ) {
        when {
            cam != null && player != null && !streamingError -> ExoPlayerView(exoPlayer = player)
            streamingError -> Text(text = "Error while streaming", color = Color.White)
            else -> Text(text = "Select a camera", color = Color.White)
        }
    }
}

@Composable
fun ExoPlayerView(exoPlayer: ExoPlayer?) {
    if(exoPlayer != null)
        AndroidView(
            factory = { context ->
                PlayerView(context).apply {
                    player = exoPlayer
                    useController = false
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    else
        Box(modifier = Modifier.background(Color.Red))
}