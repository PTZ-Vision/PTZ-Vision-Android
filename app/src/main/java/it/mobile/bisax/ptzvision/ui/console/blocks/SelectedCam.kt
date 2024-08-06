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
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.rtsp.RtspMediaSource
import androidx.media3.ui.PlayerView
import it.mobile.bisax.ptzvision.data.cam.Cam
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(UnstableApi::class)
@Composable
fun SelectedCam(
    modifier: Modifier = Modifier,
    context: Context,
    cam: Cam? = null
) {
    var player: ExoPlayer? by remember(cam) { mutableStateOf(null) }
    var streamingError by remember { mutableStateOf(false) }

    LaunchedEffect(cam) {
        if (cam != null) {
            withContext(Dispatchers.Main) {
                val newPlayer = ExoPlayer.Builder(context).build()
                newPlayer.addListener(object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        player?.stop()
                        player?.release()
                        streamingError = true
                    }
                })
                val mediaItem = MediaItem.fromUri("rtsp://${cam.ip}:${cam.streamPort}/live")
                try {
                    val rtspMediaSource = RtspMediaSource
                        .Factory()
                        .setForceUseRtpTcp(true)
                        .createMediaSource(mediaItem)
                    newPlayer.addMediaSource(rtspMediaSource)
                    newPlayer.prepare()
                    newPlayer.play()
                    player = newPlayer
                } catch (e: Exception) {
                    newPlayer.release()
                    Log.d("SelectedCam", "Error: ${e.message}")
                    streamingError = true
                }
            }
        }
        else{
            player?.release()
            player = null
            streamingError = false
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            player?.stop()
            player?.release()
        }
    }

    Box(
        modifier = modifier
            .background(Color(0xFF666666))
    ) {
        when {
            cam != null && player != null && !streamingError -> ExoPlayerView(exoPlayer = player!!)
            streamingError -> Text(text = "Error while streaming", color = Color.White)
            else -> Text(text = "Select a camera", color = Color.White)
        }
    }
}

@Composable
fun ExoPlayerView(exoPlayer: ExoPlayer) {
    AndroidView(
        factory = { context ->
            PlayerView(context).apply {
                player = exoPlayer
                useController = false
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}