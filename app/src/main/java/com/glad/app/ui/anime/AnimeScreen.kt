package com.glad.app.ui.anime

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.glad.app.data.local.AnimeEntry
import com.glad.app.data.local.AnimeStatus
import com.glad.app.data.remote.AnimeSummary
import com.glad.app.data.remote.UiState
import com.glad.app.ui.Ic
import com.glad.app.ui.components.ErrorState
import com.glad.app.ui.components.GladCard
import com.glad.app.ui.components.GladScreen
import com.glad.app.ui.components.GladTabs
import com.glad.app.ui.components.LoadingBox
import com.glad.app.ui.components.RemoteImage
import com.glad.app.ui.components.StateMessage
import com.glad.app.ui.components.gladViewModel

@Composable
fun AnimeScreen(onOpen: (Int) -> Unit) {
    val vm = gladViewModel { AnimeViewModel(it) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val library by vm.library.collectAsStateWithLifecycle()

    GladScreen("Anime", subtitle = "Powered by MyAnimeList data via Jikan") {
        GladTabs(listOf("Discover", "My list (${library.size})"), tab, { tab = it })
        if (tab == 0) DiscoverTab(vm, library, onOpen) else LibraryTab(library, onOpen)
    }
}

@Composable
private fun DiscoverTab(vm: AnimeViewModel, library: List<AnimeEntry>, onOpen: (Int) -> Unit) {
    val inLibrary = remember(library) { library.associateBy { it.malId } }

    OutlinedTextField(
        value = vm.query,
        onValueChange = vm::updateQuery,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        placeholder = { Text("Search anime") },
        leadingIcon = { Icon(Ic.Search, null) },
        trailingIcon = {
            if (vm.query.isNotEmpty()) {
                IconButton(onClick = { vm.updateQuery("") }) { Icon(Ic.Close, "Clear search") }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(50)
    )
    if (vm.query.isBlank()) {
        LazyRow(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(AnimeFeed.entries.toList()) { f ->
                FilterChip(selected = vm.feed == f, onClick = { vm.selectFeed(f) }, label = { Text(f.label) })
            }
        }
    }

    when (val s = vm.state) {
        UiState.Loading -> LoadingBox()
        is UiState.Error -> ErrorState(s.message, onRetry = vm::refresh)
        is UiState.Success -> {
            if (s.data.isEmpty()) {
                StateMessage(Ic.Search, "No results", "Try a different title.")
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(140.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(s.data, key = { it.malId }) { anime ->
                        PosterCard(anime, inLibrary[anime.malId], onClick = { onOpen(anime.malId) })
                    }
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        when {
                            vm.loadingMore -> LoadingBox()
                            vm.moreError != null -> ErrorState(vm.moreError.orEmpty(), onRetry = vm::loadMore)
                            vm.hasNext -> TextButton(onClick = vm::loadMore, modifier = Modifier.fillMaxWidth()) {
                                Text("Load more")
                            }
                            else -> Unit
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PosterCard(anime: AnimeSummary, entry: AnimeEntry?, onClick: () -> Unit) {
    GladCard(onClick = onClick) {
        Column {
            Box(Modifier.fillMaxWidth().aspectRatio(0.7f)) {
                RemoteImage(anime.imageUrl, Modifier.fillMaxSize(), anime.title)
                if (anime.score != null) {
                    Row(
                        Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Ic.Star, null, tint = Color(0xFFFFC145), modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("%.1f".format(anime.score), color = Color.White, style = MaterialTheme.typography.labelSmall)
                    }
                }
                if (entry != null) {
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Ic.Check, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(14.dp))
                    }
                }
            }
            Column(Modifier.padding(10.dp)) {
                Text(anime.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                val meta = listOfNotNull(anime.type, anime.year?.toString(), if (anime.episodes > 0) "${anime.episodes} eps" else null)
                    .joinToString(" · ")
                if (meta.isNotEmpty()) {
                    Text(meta, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun LibraryTab(library: List<AnimeEntry>, onOpen: (Int) -> Unit) {
    var filter by rememberSaveable { mutableStateOf("ALL") }
    val filtered = remember(library, filter) {
        if (filter == "ALL") library else library.filter { it.status == filter }
    }
    LazyRow(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { FilterChip(selected = filter == "ALL", onClick = { filter = "ALL" }, label = { Text("All") }) }
        items(AnimeStatus.all) { status ->
            FilterChip(
                selected = filter == status,
                onClick = { filter = status },
                label = { Text(AnimeStatus.label(status)) }
            )
        }
    }
    if (filtered.isEmpty()) {
        StateMessage(
            icon = Ic.Tv,
            title = if (library.isEmpty()) "Your list is empty" else "Nothing here",
            message = if (library.isEmpty()) "Open any anime and add it to start tracking episodes. Your list works offline."
            else "No anime with this status yet."
        )
        return
    }
    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(filtered, key = { it.malId }) { entry ->
            GladCard(onClick = { onOpen(entry.malId) }) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    RemoteImage(
                        entry.imageUrl,
                        Modifier.size(width = 60.dp, height = 84.dp).clip(RoundedCornerShape(14.dp))
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(entry.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(
                            AnimeStatus.label(entry.status) +
                                (if (entry.score > 0) " · ★ ${entry.score}/10" else ""),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(8.dp))
                        if (entry.totalEpisodes > 0) {
                            LinearProgressIndicator(
                                progress = { (entry.watched.toFloat() / entry.totalEpisodes).coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50))
                            )
                            Text(
                                "${entry.watched} / ${entry.totalEpisodes} episodes",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        } else {
                            Text(
                                "${entry.watched} episodes watched",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
