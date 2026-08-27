package com.missabusiness.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.missabusiness.app.navigation.ErpApp
import com.missabusiness.app.ui.theme.Erp360Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Erp360Theme {
                ErpApp()
            }
        }
    }
}
