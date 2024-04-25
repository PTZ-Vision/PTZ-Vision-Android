package it.mobile.bisax.ptzvision.ui.settings

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import it.mobile.bisax.ptzvision.R
import it.mobile.bisax.ptzvision.ui.settings.layout.Layout

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTopBar() {
    TopAppBar(
        title = { Text(text = stringResource(id = R.string.settings_route)) },
        navigationIcon = {
            IconButton(onClick = { /*TODO*/ }) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = stringResource(id = R.string.home_route)
                )
            }
        }
    )
}

@Composable
fun SettingsScreen(
    context: Context
) {
    (context as Activity).requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    Surface {
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            item{
                Text(text = "Settings")
            }
            item{
                SettingsSection(
                    modifier = Modifier,
                    title = "Layout"
                ) {
                    Row {
                        Layout(SettingsViewModel(context))
                    }
                }
            }
        }
    }
}