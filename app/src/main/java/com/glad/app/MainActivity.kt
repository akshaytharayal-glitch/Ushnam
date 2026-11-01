package com.glad.app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.glad.app.data.prefs.AppSettings
import com.glad.app.ui.nav.GladRoot
import com.glad.app.ui.theme.GladTheme
import com.glad.app.ui.theme.isDarkTheme

class MainActivity : ComponentActivity() {
    /** Route requested from a widget or the media notification; consumed by the nav host. */
    private val requestedRoute = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedRoute.value = intent?.getStringExtra(EXTRA_ROUTE)
        val store = container.settings

        setContent {
            val settings by store.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
            val dark = isDarkTheme(settings)
            DisposableEffect(dark) {
                val style = if (dark) SystemBarStyle.dark(Color.TRANSPARENT)
                else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose {}
            }
            GladTheme(settings) {
                GladRoot(
                    requestedRoute = requestedRoute.value,
                    onRouteHandled = { requestedRoute.value = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        requestedRoute.value = intent.getStringExtra(EXTRA_ROUTE)
    }

    companion object {
        const val EXTRA_ROUTE = "glad_route"
    }
}
