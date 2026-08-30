package com.missabusiness.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.missabusiness.app.navigation.ErpApp
import com.missabusiness.app.ui.screens.SplashVideoScreen
import com.missabusiness.app.ui.theme.Erp360Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Erp360Theme {
                // Vidéo d'intro jouée juste après le splash système Android,
                // puis bascule vers la coquille de navigation principale.
                var showSplashVideo by remember { mutableStateOf(true) }
                if (showSplashVideo) {
                    SplashVideoScreen(onFinished = { showSplashVideo = false })
                } else {
                    ErpApp()
                }
            }
        }
    }
}
