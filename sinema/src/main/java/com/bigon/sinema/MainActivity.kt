package com.bigon.sinema

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.bigon.sinema.ui.SinemaApp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Edge-to-edge from day one (§5.3) — enforced-default behavior on Android 15+.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            // BigonTheme is applied inside SinemaApp, where the theme picker lives.
            SinemaApp()
        }
    }
}
