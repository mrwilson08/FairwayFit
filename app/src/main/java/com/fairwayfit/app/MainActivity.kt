package com.fairwayfit.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.fairwayfit.app.ui.nav.AppRoot
import com.fairwayfit.app.ui.theme.FairwayFitTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        setContent {
            FairwayFitTheme {
                AppRoot()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Pick up Health Connect permission or data changes made outside the app.
        val app = application as FairwayFitApp
        lifecycleScope.launch { app.container.health.refresh() }
    }
}
