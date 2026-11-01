package com.glad.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.glad.app.AppContainer
import com.glad.app.data.local.AnimeEntry
import com.glad.app.ui.components.GladCard
import com.glad.app.ui.components.RemoteImage
import com.glad.app.ui.components.SectionTitle
import com.glad.app.ui.components.gladViewModel
import com.glad.app.ui.nav.Feature
import com.glad.app.ui.nav.Features
import com.glad.app.ui.theme.Accents
import com.glad.app.ui.theme.LocalBrand
import com.glad.app.ui.theme.brandTextStyle
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private data class Counts(val movies: Int, val anime: Int, val wallpapers: Int, val playlists: Int)

data class HomeUi(
    val movies: Int = 0,
    val anime: Int = 0,
    val wallpapers: Int = 0,
    val playlists: Int = 0,
    val continueWatching: List<AnimeEntry> = emptyList(),
    val recents: List<String> = emptyList()
)

class HomeViewModel(c: AppContainer) : ViewModel() {
    private val counts = combine(
        c.db.movieDao().observeWatchlistCount(),
        c.db.animeDao().observeWatchingCount(),
        c.db.wallpaperDao().observeCount(),
        c.db.playlistDao().observeCount()
    ) { m, a, w, p -> Counts(m, a, w, p) }

    val ui: StateFlow<HomeUi> = combine(
        counts,
        c.db.animeDao().observeContinueWatching(),
        c.settings.recents
    ) { n, cont, recents ->
        HomeUi(n.movies, n.anime, n.wallpapers, n.playlists, cont, recents)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUi())
}

private fun greeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..21 -> "Good evening"
        else -> "Late night vibes"
    }
}

@Composable
fun HomeScreen(onOpen: (String) -> Unit, onOpenAnime: (Int) -> Unit) {
    val vm = gladViewModel { HomeViewModel(it) }
    val ui by vm.ui.collectAsStateWithLifecycle()
    val brand = LocalBrand.current
    val date = remember { SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date()) }

    LazyColumn(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Column(Modifier.statusBarsPadding().padding(horizontal = 20.dp, vertical = 16.dp)) {
                Text(
                    date.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.4.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(greeting(), style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Glad you're here.",
                    style = brandTextStyle(MaterialTheme.typography.headlineMedium)
                )
            }
        }

        item {
            Box(
                Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(brand.soft(0.26f))
                    .padding(20.dp)
            ) {
                Column {
                    Text("Your space", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Stat("Movies to watch", ui.movies, Modifier.weight(1f))
                        Stat("Anime watching", ui.anime, Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Stat("Saved walls", ui.wallpapers, Modifier.weight(1f))
                        Stat("Playlists", ui.playlists, Modifier.weight(1f))
                    }
                }
            }
        }

        if (ui.continueWatching.isNotEmpty()) {
            item { SectionTitle("Continue watching", Modifier.padding(top = 12.dp)) }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(ui.continueWatching, key = { it.malId }) { entry ->
                        ContinueCard(entry) { onOpenAnime(entry.malId) }
                    }
                }
            }
        }

        val recentFeatures = ui.recents.mapNotNull { Features.byRoute(it) }
        if (recentFeatures.isNotEmpty()) {
            item { SectionTitle("Recently used", Modifier.padding(top = 12.dp)) }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(recentFeatures, key = { it.route }) { f ->
                        GladCard(onClick = { onOpen(f.route) }) {
                            Row(
                                Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(f.icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(10.dp))
                                Text(f.title, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }
        }

        item { SectionTitle("Everything in Glad", Modifier.padding(top = 12.dp)) }
        val rows = Features.all.chunked(2)
        items(rows.size) { rowIndex ->
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rows[rowIndex].forEachIndexed { col, feature ->
                    FeatureTile(feature, rowIndex * 2 + col, Modifier.weight(1f)) { onOpen(feature.route) }
                }
                if (rows[rowIndex].size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: Int, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.35f))
            .padding(14.dp)
    ) {
        Text(value.toString(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun FeatureTile(feature: Feature, index: Int, modifier: Modifier, onClick: () -> Unit) {
    val tint = Accents[(index + 1) % Accents.size].color
    GladCard(modifier = modifier, onClick = onClick) {
        Column(Modifier.padding(16.dp)) {
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(tint.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(feature.icon, null, tint = tint)
            }
            Spacer(Modifier.height(14.dp))
            Text(feature.title, style = MaterialTheme.typography.titleMedium)
            Text(
                feature.tagline,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ContinueCard(entry: AnimeEntry, onClick: () -> Unit) {
    GladCard(modifier = Modifier.width(260.dp), onClick = onClick) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            RemoteImage(
                entry.imageUrl,
                Modifier.size(width = 64.dp, height = 90.dp).clip(RoundedCornerShape(14.dp))
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(entry.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(6.dp))
                val total = entry.totalEpisodes
                Text(
                    if (total > 0) "Episode ${entry.watched} of $total" else "Episode ${entry.watched}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (total > 0) {
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { (entry.watched.toFloat() / total).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50))
                    )
                }
            }
        }
    }
}
