package com.glad.app.ui.anime

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.glad.app.ui.components.LoadingBox
import com.glad.app.ui.components.RemoteImage
import com.glad.app.ui.components.gladViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AnimeDetailScreen(malId: Int, onBack: () -> Unit) {
    val vm = gladViewModel(key = "anime_$malId") { AnimeDetailViewModel(it, malId) }
    val entry by vm.entry.collectAsStateWithLifecycle()
    val detail = vm.detail

    GladScreen(
        title = entry?.title ?: (detail as? UiState.Success)?.data?.title ?: "Anime",
        onBack = onBack
    ) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 32.dp)) {
            val info: AnimeSummary? = (detail as? UiState.Success)?.data
            val imageUrl = info?.imageUrl ?: entry?.imageUrl

            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                RemoteImage(
                    imageUrl,
                    Modifier.size(width = 130.dp, height = 190.dp).clip(RoundedCornerShape(20.dp))
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (info != null) {
                        info.score?.let {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Ic.Star, null, tint = androidx.compose.ui.graphics.Color(0xFFFFC145), modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("%.2f".format(it), style = MaterialTheme.typography.titleMedium)
                            }
                        }
                        info.rank?.let { Text("Rank #$it", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary) }
                        val meta = listOfNotNull(
                            info.type,
                            info.year?.toString(),
                            if (info.episodes > 0) "${info.episodes} episodes" else null,
                            info.duration
                        )
                        meta.forEach {
                            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        info.status?.let {
                            Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (info.studios.isNotEmpty()) {
                            Text(
                                info.studios.joinToString(", "),
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            TrackingCard(entry = entry, info = info, vm = vm)
            Spacer(Modifier.height(8.dp))

            when (detail) {
                UiState.Loading -> LoadingBox()
                is UiState.Error -> {
                    if (entry != null) {
                        Text(
                            "Showing your saved details. ${detail.message}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                        )
                        TextButton(onClick = vm::load, modifier = Modifier.padding(horizontal = 12.dp)) { Text("Retry") }
                    } else {
                        ErrorState(detail.message, onRetry = vm::load)
                    }
                }
                is UiState.Success -> {
                    val d = detail.data
                    if (d.genres.isNotEmpty()) {
                        FlowRow(
                            Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            d.genres.forEach { AssistChip(onClick = {}, label = { Text(it) }) }
                        }
                    }
                    d.synopsis?.let { Synopsis(it) }
                    d.rating?.let {
                        Text(
                            "Rated: $it",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Synopsis(text: String) {
    var expanded by remember { mutableStateOf(false) }
    Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp).animateContentSize()) {
        Text("Synopsis", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = if (expanded) Int.MAX_VALUE else 5,
            overflow = TextOverflow.Ellipsis
        )
        TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "Show less" else "Read more") }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TrackingCard(entry: AnimeEntry?, info: AnimeSummary?, vm: AnimeDetailViewModel) {
    GladCard(Modifier.padding(horizontal = 16.dp).fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (entry == null) "Add to your list" else "Your progress", style = MaterialTheme.typography.titleMedium)

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AnimeStatus.all.forEach { status ->
                    FilterChip(
                        selected = entry?.status == status,
                        onClick = { vm.setStatus(status) },
                        label = { Text(AnimeStatus.label(status)) }
                    )
                }
            }

            if (entry != null) {
                val total = entry.totalEpisodes
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalIconButton(onClick = { vm.setProgress(entry.watched - 1) }, enabled = entry.watched > 0) {
                        Icon(Ic.Remove, "One episode less")
                    }
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            if (total > 0) "Episode ${entry.watched} / $total" else "Episode ${entry.watched}",
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (total > 0) {
                            LinearProgressIndicator(
                                progress = { (entry.watched.toFloat() / total).coerceIn(0f, 1f) },
                                modifier = Modifier.padding(top = 6.dp).fillMaxWidth(0.7f).height(6.dp).clip(RoundedCornerShape(50))
                            )
                        }
                    }
                    FilledTonalIconButton(
                        onClick = { vm.setProgress(entry.watched + 1) },
                        enabled = total == 0 || entry.watched < total
                    ) {
                        Icon(Ic.Add, "One episode more")
                    }
                }

                Text(
                    if (entry.score > 0) "Your score: ${entry.score}/10" else "Your score: not rated",
                    style = MaterialTheme.typography.labelLarge
                )
                Slider(
                    value = entry.score.toFloat(),
                    onValueChange = { vm.setScore(it.toInt()) },
                    valueRange = 0f..10f,
                    steps = 9
                )

                OutlinedButton(onClick = vm::remove, modifier = Modifier.fillMaxWidth()) {
                    Icon(Ic.Delete, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Remove from list")
                }
            } else {
                Button(
                    onClick = { vm.setStatus(AnimeStatus.PLAN) },
                    enabled = info != null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Ic.Add, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (info == null) "Waiting for details…" else "Add as plan to watch")
                }
            }
        }
    }
}
