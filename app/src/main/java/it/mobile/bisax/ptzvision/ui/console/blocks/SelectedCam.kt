package it.mobile.bisax.ptzvision.ui.console.blocks

import android.content.Context
import android.util.Log
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player.Listener
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.rtsp.RtspMediaSource
import androidx.media3.ui.PlayerView
import it.mobile.bisax.ptzvision.data.cam.Cam

@OptIn(UnstableApi::class)
@Composable
fun SelectedCam(
    modifier: Modifier = Modifier,
    context: Context,
    cam: Cam? = null
) {
    val player by remember(cam) { mutableStateOf(ExoPlayer.Builder(context).build())}
    var streamingError by remember { mutableStateOf(false) }
    player.addListener(object: Listener{
        override fun onPlayerError(error: PlaybackException){
            player.stop()
            player.release()
            streamingError = true
        }
    })
    Log.d("PLAYER", "Player: $player")
    if(cam != null) {
        val mediaItem = MediaItem.fromUri("rtsp://${cam.ip}:${cam.streamPort}/2")

        DisposableEffect(player) {
            onDispose {
                player.release()
            }
        }

        try {
            val rtspMediaSource =
                RtspMediaSource.Factory().setForceUseRtpTcp(true).createMediaSource(mediaItem)
            player.addMediaSource(rtspMediaSource)

            player.prepare()
            player.play()
        } catch (e: Exception) {
            player.release()
            Log.d("SelectedCam", "Error: ${e.message}")
        }
    }
    Box(
        modifier = modifier
            .background(Color(0xFF666666))
    ){
        if(cam != null && !streamingError)
            ExoPlayerView(exoPlayer = player)
        else if (streamingError){
            Text(
                text = "Error while streaming",
                color = Color.White,
            )
        }
        else
            Text(
                text = "Select a camera",
                color = Color.White,
            )
    }
}

@Composable
fun ExoPlayerView(exoPlayer: ExoPlayer) {
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        AndroidView(
            factory = { context ->
                PlayerView(context).apply {
                    player = exoPlayer
                    useController = false
                }
            },
            modifier = Modifier.fillMaxWidth() // Adjust as needed
        )
    }
}