package com.bigon.sinema

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.bigon.sinema.ui.SinemaApp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Before super.onCreate, and before anything else: the call swaps the
        // launch theme for postSplashScreenTheme, so running it later would let
        // the splash's window background become the app's.
        //
        // Android 12 shows a splash regardless; this is what gives API 26–30
        // the same thing instead of a blank window while the process starts.
        installSplashScreen()

        // Edge-to-edge from day one (§5.3) — enforced-default behavior on Android 15+.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            // BigonTheme is applied inside SinemaApp, where the theme picker lives.
            SinemaApp()
        }
    }
}
