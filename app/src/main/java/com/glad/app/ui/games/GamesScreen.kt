package com.glad.app.ui.games

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalContext
import com.glad.app.container
import com.glad.app.data.prefs.GameStats
import com.glad.app.ui.Ic
import com.glad.app.ui.components.GladCard
import com.glad.app.ui.components.GladScreen
import com.glad.app.ui.nav.Routes

@Composable
fun GamesScreen(onOpen: (String) -> Unit, onBack: () -> Unit) {
    val settings = LocalContext.current.container.settings
    val stats by remember { settings.gameStats }.collectAsStateWithLifecycle(initialValue = GameStats())

    GladScreen("Games", subtitle = "All offline, scores saved on this device", onBack = onBack) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            GameCard("2048", "Slide and merge tiles", "Best score ${stats.best2048}", "2", Routes.GAME_2048, onOpen)
            GameCard(
                "Memory Match", "Find every matching pair",
                if (stats.bestMemoryMoves > 0) "Best: ${stats.bestMemoryMoves} moves" else "No record yet",
                "M", Routes.GAME_MEMORY, onOpen
            )
            GameCard(
                "Tic-Tac-Toe", "Beat the computer",
                "W ${stats.tttWins} · D ${stats.tttDraws} · L ${stats.tttLosses}",
                "X", Routes.GAME_TTT, onOpen
            )
        }
    }
}

@Composable
private fun GameCard(title: String, tagline: String, record: String, glyph: String, route: String, onOpen: (String) -> Unit) {
    GladCard(Modifier.fillMaxWidth(), onClick = { onOpen(route) }) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(56.dp).clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(glyph, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(tagline, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(record, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            Icon(Ic.Chevron, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
