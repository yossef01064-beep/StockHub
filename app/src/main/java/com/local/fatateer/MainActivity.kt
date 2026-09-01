package com.local.fatateer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import com.local.fatateer.ui.FatateerApp
import com.local.fatateer.ui.locale.LocalAppStrings
import com.local.fatateer.ui.locale.LocalSettingsController
import com.local.fatateer.ui.locale.rememberSettingsController
import com.local.fatateer.ui.theme.FatateerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings = rememberSettingsController()
            val systemDark = isSystemInDarkTheme()
            // force recomposition when settings change
            @Suppress("UNUSED_VARIABLE")
            val themeTick = settings.themeMode
            @Suppress("UNUSED_VARIABLE")
            val langTick = settings.language

            val dark = settings.isDark(systemDark)
            val strings = settings.strings

            FatateerTheme(darkTheme = dark) {
                CompositionLocalProvider(
                    LocalSettingsController provides settings,
                    LocalAppStrings provides strings
                ) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        FatateerApp(isDark = dark)
                    }
                }
            }
        }
    }
}
