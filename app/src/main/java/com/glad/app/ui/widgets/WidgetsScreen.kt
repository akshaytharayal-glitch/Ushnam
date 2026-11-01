package com.glad.app.ui.widgets

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.glad.app.ui.Ic
import com.glad.app.ui.components.GladCard
import com.glad.app.ui.components.GladScreen
import com.glad.app.widget.DashboardWidgetProvider
import com.glad.app.widget.ShortcutsWidgetProvider
import com.glad.app.widget.WidgetUpdater

@Composable
fun WidgetsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val manager = remember { AppWidgetManager.getInstance(context) }
    var dashboardCount by remember { mutableIntStateOf(0) }
    var shortcutCount by remember { mutableIntStateOf(0) }
    val pinSupported = remember { WidgetUpdater.isPinSupported(context) }
    var tick by remember { mutableIntStateOf(0) }

    LaunchedEffect(tick) {
        dashboardCount = manager.getAppWidgetIds(ComponentName(context, DashboardWidgetProvider::class.java)).size
        shortcutCount = manager.getAppWidgetIds(ComponentName(context, ShortcutsWidgetProvider::class.java)).size
    }

    fun pin(provider: Class<*>) {
        val ok = WidgetUpdater.requestPin(context, provider)
        if (!ok) {
            Toast.makeText(
                context,
                "Your launcher can't add widgets from apps. Long-press the home screen, choose Widgets, then Glad.",
                Toast.LENGTH_LONG
            ).show()
        }
        tick++
    }

    GladScreen("Widgets", subtitle = "Put Glad on your home screen", onBack = onBack) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            WidgetCard(
                title = "Glad Today",
                description = "Clock, date and live counts of movies to watch and anime in progress. Tap to open Glad.",
                added = dashboardCount,
                icon = Ic.Widgets,
                onAdd = { pin(DashboardWidgetProvider::class.java) },
                canPin = pinSupported
            )
            WidgetCard(
                title = "Glad Shortcuts",
                description = "Four one-tap shortcuts that open Wallpapers, Anime, Music or Games directly.",
                added = shortcutCount,
                icon = Ic.Tune,
                onAdd = { pin(ShortcutsWidgetProvider::class.java) },
                canPin = pinSupported
            )
            Text(
                if (pinSupported) "Tap Add and confirm in the dialog your launcher shows."
                else "Your launcher doesn't support one-tap adding. Long-press the home screen, open Widgets and pick Glad.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WidgetCard(
    title: String,
    description: String,
    added: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onAdd: () -> Unit,
    canPin: Boolean
) {
    GladCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (added == 0) "Not on your home screen" else "On your home screen ×$added",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (added == 0) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                    )
                }
            }
            Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(2.dp))
            Button(onClick = onAdd, enabled = canPin, modifier = Modifier.fillMaxWidth()) {
                Text(if (added == 0) "Add to home screen" else "Add another")
            }
        }
    }
}
