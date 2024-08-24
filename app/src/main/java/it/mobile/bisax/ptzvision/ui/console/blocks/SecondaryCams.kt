package it.mobile.bisax.ptzvision.ui.console.blocks

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import it.mobile.bisax.ptzvision.R
import it.mobile.bisax.ptzvision.ui.console.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SecondaryCams(
    modifier: Modifier = Modifier,
    mainViewModel: MainViewModel,
    onClick: () -> Unit
) {
    val uiState by mainViewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    Row(modifier = modifier) {
        val vibe = LocalContext.current.getSystemService(Vibrator::class.java) as Vibrator
        for (i in 1..3) {
            var currentImage by remember { mutableStateOf<ImageBitmap?>(null) }
            var errorOccurred by remember { mutableStateOf(false) }  // Stato per l'errore

            Column(
                modifier = Modifier
                    .weight(1f)
//                    .background(MaterialTheme.colorScheme.primary)
                    .fillMaxHeight()
                    .clickable {
                        if (uiState.activeCams.getOrNull(i) != null) {
                            coroutineScope.launch {
                                mainViewModel.setNewActiveCam(i)
                            }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                vibe.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
                            }
                        } else {
                            onClick()
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                vibe.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK))
                            }
                        }
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f), contentAlignment = Alignment.Center) {
                    if (uiState.activeCams.getOrNull(i) != null) {
                        LaunchedEffect(uiState.activeCams[i]) {
                            while (true) {
                                try {
                                    val loader = ImageLoader(context)
                                    val request = ImageRequest.Builder(context)
                                        .data("http://${uiState.activeCams[i]!!.ip}:${uiState.activeCams[i]!!.httpPort}/snapshot.jpg?time=${System.currentTimeMillis()}")
                                        .build()

                                    val result = (loader.execute(request) as SuccessResult).drawable
                                    val bitmap =
                                        (result as? android.graphics.drawable.BitmapDrawable)?.bitmap

                                    if (bitmap != null) {
                                        currentImage = bitmap.asImageBitmap()
                                        errorOccurred =
                                            false // Nessun errore, immagine caricata correttamente
                                    } else {
                                        errorOccurred = true // Se non riesce a caricare l'immagine
                                    }
                                } catch (e: Exception) {
                                    errorOccurred = true // Imposta errore se la richiesta fallisce
                                }
                                delay(5000L) // 5 secondi di attesa
                            }
                        }
                        Crossfade(
                            targetState = currentImage,
                            modifier = Modifier
                        ) { image ->
                            if (!errorOccurred && image != null) {
                                Image(
                                    bitmap = image,
                                    contentDescription = null,
                                    contentScale = ContentScale.Fit,
                                )
                            } else {
                                Image(
                                    painter = painterResource(id = R.drawable.videocam_on), // Fallback image
                                    contentDescription = "Fallback Camera Image",
                                    colorFilter = ColorFilter.tint(Color(0xFF00FFFF)),
                                    modifier = Modifier.size(50.dp)
                                )
                            }
                        }
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.videocam_off),
                            contentDescription = "No Camera",
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.error),
                            modifier = Modifier.size(50.dp)
                        )
                    }
                }

                Text(
                    text = uiState.activeCams.getOrNull(i)?.name ?: "No Camera",
                    color = if (uiState.activeCams.getOrNull(i) != null) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}
