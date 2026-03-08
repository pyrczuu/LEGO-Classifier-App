package com.example.legoclassifierapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.modifier.modifierLocalConsumer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.legoclassifierapp.ui.theme.LEGOClassifierAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //enableEdgeToEdge()
        setContent {
            LEGOClassifierAppTheme {
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = AddPhotoScreen
                ) {
                    composable<AddPhotoScreen> {
                        Scaffold(
                            modifier = Modifier
                                .fillMaxSize(),
                            bottomBar = {
                                BottomNav(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(84.dp),
                                    navController = navController
                                )
                            }) { innerPadding ->
                            ColPhotoButtons(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                            )
                        }
                    }
                    composable<InventoryScreen> {
                        Scaffold(
                            modifier = Modifier
                                .fillMaxSize(),
                            bottomBar = {
                                BottomNav(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(84.dp),
                                    navController = navController
                                )
                            }) { innerPadding ->
                            InventoryList(
                                modifier = Modifier
                                    .padding(innerPadding))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ColPhotoButtons(
    modifier: Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .weight(1f),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ButtonTakePhoto(
                modifier = Modifier
                    .fillMaxSize(),
                onClick = {},
            )
        }
        Row(
            modifier = Modifier
                .weight(1f),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ButtonChoosePhoto(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                onClick = {}
            )
        }
    }
}

@Composable
fun ButtonTakePhoto(
    modifier: Modifier,
    onClick: () -> Unit
){
    Button(
        onClick = onClick,
        modifier = modifier
    ){
        Text(
            text = "Take a photo"
        )
    }
}

@Composable
fun ButtonChoosePhoto(
    modifier: Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,

    ) {
        Text(
            text = "Choose a photo"
        )
    }
}

@Composable
fun BottomNav(
    modifier: Modifier,
    navController: NavController,
    ) {
    var checked by remember { mutableStateOf(false) }
    Row(
        modifier = modifier
            .background(color = Color.Yellow),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SwitchNavigation(
            checked = checked,
            onCheckedChange = { newValue -> checked = newValue

                if (newValue) {
                    navController.navigate(AddPhotoScreen)
                } else {
                    navController.navigate(InventoryScreen)
                }
            }
        )
    }
}

@Composable
fun SwitchNavigation(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    var checked by remember { mutableStateOf(true) }
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.LightGray,
            checkedTrackColor = Color.White,
            uncheckedThumbColor = Color.DarkGray,
            uncheckedTrackColor = Color.White,
        ),
        modifier = Modifier
            .fillMaxSize()
            .scale(2f),
        thumbContent = if (checked) {
            {
                Icon(
                    painter = painterResource(R.drawable.storage),
                    contentDescription = null,
                    modifier = Modifier.size(SwitchDefaults.IconSize),
                )
            }
        } else {
            {
                Icon(
                    painter = painterResource(R.drawable.photo),
                    contentDescription = null,
                    modifier = Modifier.size(SwitchDefaults.IconSize),
                )
            }
        }
    )
}

@Composable
fun InventoryList(
    modifier: Modifier
) {
    Text(text = "Placeholder")
}