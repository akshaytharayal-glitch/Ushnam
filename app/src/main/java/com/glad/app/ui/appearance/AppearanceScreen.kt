package com.glad.app.ui.appearance

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.glad.app.container
import com.glad.app.data.prefs.AppSettings
import com.glad.app.data.prefs.ThemeMode
import com.glad.app.ui.Ic
import com.glad.app.ui.components.GladCard
import com.glad.app.ui.components.GladScreen
import com.glad.app.ui.components.GladTabs
import com.glad.app.ui.components.SectionTitle
import com.glad.app.ui.theme.Accents
import com.glad.app.ui.theme.LocalBrand
import kotlinx.coroutines.launch

@Composable
fun AppearanceScreen(onBack: () -> Unit) {
    val store = LocalContext.current.container.settings
    val settings by store.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
    val scope = rememberCoroutineScope()
    val dynamicSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val modes = listOf(ThemeMode.DARK, ThemeMode.LIGHT, ThemeMode.SYSTEM)

    GladScreen("Appearance", subtitle = "Make Glad yours", onBack = onBack) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 32.dp)) {
            SectionTitle("Theme")
            GladTabs(
                listOf("Dark", "Light", "System"),
                modes.indexOf(settings.themeMode).coerceAtLeast(0),
                { scope.launch { store.setThemeMode(modes[it]) } }
            )

            SectionTitle("Accent colour", Modifier.padding(top = 12.dp))
            LazyRow(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(Accents) { index, accent ->
                    val selected = index == settings.accentIndex && !(settings.dynamicColor && dynamicSupported)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { scope.launch { store.setAccent(index); store.setDynamicColor(false) } }
                    ) {
                        Box(
                            Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(accent.color)
                                .then(if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.onBackground, CircleShape) else Modifier),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) Icon(Ic.Check, null, tint = Color(0xFF1A1226))
                        }
                        Text(accent.name, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }

            SectionTitle("Options", Modifier.padding(top = 16.dp))
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ToggleRow(
                    "Pure black (AMOLED)",
                    "True black backgrounds in dark mode to save battery on OLED screens.",
                    settings.amoled
                ) { scope.launch { store.setAmoled(it) } }
                ToggleRow(
                    "Match wallpaper colours",
                    if (dynamicSupported) "Use Material You colours from your wallpaper (Android 12+)."
                    else "Needs Android 12 or newer.",
                    settings.dynamicColor && dynamicSupported,
                    enabled = dynamicSupported
                ) { scope.launch { store.setDynamicColor(it) } }
            }

            SectionTitle("Preview", Modifier.padding(top = 16.dp))
            GladCard(Modifier.padding(horizontal = 16.dp).fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Glad", style = MaterialTheme.typography.headlineMedium)
                    Box(
                        Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(50)).background(LocalBrand.current.brush)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary,
                            MaterialTheme.colorScheme.tertiary,
                            MaterialTheme.colorScheme.primaryContainer
                        ).forEach { Box(Modifier.size(34.dp).clip(CircleShape).background(it)) }
                    }
                    Text(
                        "Cards, chips and controls everywhere in the app follow these choices instantly.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    GladCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(12.dp))
            Switch(checked, onChange, enabled = enabled)
        }
    }
}
