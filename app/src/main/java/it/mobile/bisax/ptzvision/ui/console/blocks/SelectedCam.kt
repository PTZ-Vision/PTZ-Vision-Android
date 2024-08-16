package it.mobile.bisax.ptzvision.ui.console.blocks

import android.content.Context
import android.util.Log
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
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
    lifecycleOwner: LifecycleOwner,
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

    fun initPlayer(currentCam: Cam){
        resetPlayer()
        val loadControl = DefaultLoadControl
            .Builder()
            .setBufferDurationsMs(
                500,
                1000,
                250,
                500
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
        val mediaItem = MediaItem.fromUri("rtsp://${currentCam.ip}:${currentCam.streamPort}/live")
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

    // Gestione del ciclo di vita per la pausa e il rilascio del player
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    resetPlayer() // Ferma e rilascia il player quando l'Activity va in pausa
                }
                Lifecycle.Event.ON_RESUME -> {
                    //player?.play() // Riprendi la riproduzione quando l'Activity riprende
                }
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            resetPlayer() // Assicurati che il player sia rilasciato quando il composable viene smontato
        }
    }

    LaunchedEffect(cam) {
        if (cam != null) {
            initPlayer(cam)
        }
    }

    Box(
        modifier = modifier
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if(cam != null){
            if(player != null && !streamingError){
                Text(text = "Retry", color = Color.White)
                ExoPlayerView(exoPlayer = player!!)
            }
            else if(streamingError) {
                Text(text = "Error while streaming", color = Color.White)

                Button(onClick = { initPlayer(cam) }) {
                    Text(text = "Retry", color = Color.White)
                }
            }
            else{
                Button(onClick = { initPlayer(cam) }) {
                    Text(text = "Retry", color = Color.White)
                }
            }
        }
        else{
            Text(text = "No camera selected", color = Color.White)
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
        modifier = Modifier.fillMaxSize(),
        update = { playerView ->
            playerView.player = exoPlayer
        },
        onReset = { playerView ->
            playerView.player = null
        },
        onRelease = { playerView ->
            playerView.player = null
        }
    )
}