package it.mobile.bisax.ptzvision.ui.home

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.mobile.bisax.ptzvision.R

@SuppressLint("SourceLockedOrientationActivity")
@Composable
fun HomeScreen(
    goToConsole: () -> Unit,
    goToSettings: () -> Unit,
    windowSize: WindowSizeClass
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(0.dp, 20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ){
        Text(
            text = "PTZ\nVision",
            lineHeight = 54.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 50.sp,
            modifier = Modifier
                .then(
                    if(windowSize.heightSizeClass > WindowHeightSizeClass.Compact){
                        Modifier.padding(0.dp, 0.dp, 0.dp, 30.dp)
                    }
                    else{
                        Modifier.padding(0.dp, 0.dp, 0.dp, 10.dp)
                    }
                )
        )
        Column(
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .then(
                    if(windowSize.heightSizeClass == WindowHeightSizeClass.Compact){
                        Modifier.height(200.dp)
                    }
                    else{
                        Modifier.height(300.dp)
                    }
                )
        ){
            Button(
                onClick = goToConsole,
                modifier = Modifier
                    .then(
                        if(windowSize.heightSizeClass == WindowHeightSizeClass.Compact){
                            Modifier.height(75.dp).width(250.dp)
                        }
                        else{
                            Modifier.height(100.dp).width(300.dp)
                        }
                    ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start,
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.joystick),
                        contentDescription = "Settings",
                        modifier = Modifier
                            .padding(0.dp, 0.dp, 20.dp, 0.dp)
                            .size(40.dp)
                    )

                    Text(
                        text = stringResource(R.string.console_btn),
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
            }
            Button(
                onClick = goToSettings,
                modifier = Modifier
                    .then(
                        if(windowSize.heightSizeClass == WindowHeightSizeClass.Compact){
                            Modifier.height(75.dp).width(250.dp)
                        }
                        else{
                            Modifier.height(100.dp).width(300.dp)
                        }
                    ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                    disabledContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                    disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start,
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.settings),
                        contentDescription = "Settings",
                        modifier = Modifier
                            .padding(0.dp, 0.dp, 20.dp, 0.dp)
                            .size(40.dp)
                    )

                    Text(
                        text = stringResource(R.string.settings_btn),
                        style = MaterialTheme.typography.titleLarge,

                        )
                }
            }
        }
    }
}