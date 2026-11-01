package com.glad.app.ui.music

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import com.glad.app.AppContainer
import com.glad.app.container
import com.glad.app.data.local.Playlist
import com.glad.app.data.local.PlaylistTrack
import com.glad.app.music.PlayerState
import com.glad.app.music.Track
import com.glad.app.music.formatDuration
import com.glad.app.ui.Ic
import com.glad.app.ui.components.GladCard
import com.glad.app.ui.components.GladScreen
import com.glad.app.ui.components.GladTabs
import com.glad.app.ui.components.LoadingBox
import com.glad.app.ui.components.StateMessage
import com.glad.app.ui.components.gladViewModel
import com.glad.app.ui.theme.LocalBrand
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MusicViewModel(private val c: AppContainer) : ViewModel() {
    var tracks by mutableStateOf<List<Track>>(emptyList())
        private set
    var loading by mutableStateOf(false)
        private set
    var loaded by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    val playlists: StateFlow<List<Playlist>> = c.db.playlistDao().observePlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val playlistTracks: StateFlow<List<PlaylistTrack>> = c.db.playlistDao().observeAllTracks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val player: StateFlow<PlayerState> = c.music.state

    fun load() {
        if (loading) return
        loading = true
        error = null
        viewModelScope.launch {
            try {
                tracks = c.audio.loadTracks()
                loaded = true
            } catch (e: Exception) {
                error = "Couldn't read your music library."
            }
            loading = false
        }
    }

    fun play(queue: List<Track>, index: Int) = c.music.playQueue(queue, index)

    fun createPlaylist(name: String) {
        viewModelScope.launch { c.db.playlistDao().insert(Playlist(name = name.trim(), createdAt = System.currentTimeMillis())) }
    }

    fun deletePlaylist(id: Long) {
        viewModelScope.launch { c.db.playlistDao().delete(id) }
    }

    fun rename(id: Long, name: String) {
        viewModelScope.launch { c.db.playlistDao().rename(id, name.trim()) }
    }

    fun addToPlaylist(playlistId: Long, trackId: Long) {
        viewModelScope.launch { c.db.playlistDao().addTrack(PlaylistTrack(playlistId, trackId, System.currentTimeMillis())) }
    }

    fun removeFromPlaylist(playlistId: Long, trackId: Long) {
        viewModelScope.launch { c.db.playlistDao().removeTrack(playlistId, trackId) }
    }
}

@Composable
private fun rememberAudioPermission(onResult: (Boolean) -> Unit): Pair<Boolean, () -> Unit> {
    val context = LocalContext.current
    val permission = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
    val granted = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission(), onResult)
    return granted to { launcher.launch(permission) }
}

@Composable
fun MusicScreen(onBack: () -> Unit, onOpenPlaylist: (Long) -> Unit) {
    val vm = gladViewModel { MusicViewModel(it) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var granted by remember { mutableStateOf(false) }
    val (alreadyGranted, request) = rememberAudioPermission { ok -> granted = ok; if (ok) vm.load() }

    LaunchedEffect(alreadyGranted) {
        if (alreadyGranted) {
            granted = true
            if (!vm.loaded) vm.load()
        }
    }

    val playlists by vm.playlists.collectAsStateWithLifecycle()
    val playlistTracks by vm.playlistTracks.collectAsStateWithLifecycle()
    val player by vm.player.collectAsStateWithLifecycle()
    var creating by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        GladScreen(
            "Music",
            subtitle = if (vm.loaded) "${vm.tracks.size} songs on this device" else "Your local library",
            onBack = onBack,
            actions = {
                if (granted) IconButton(onClick = vm::load) { Icon(Ic.Refresh, "Rescan library") }
            }
        ) {
            if (!granted) {
                StateMessage(
                    icon = Ic.Music,
                    title = "Allow access to your music",
                    message = "Glad reads audio files stored on this device to play them. Nothing is uploaded.",
                    actionLabel = "Allow access",
                    onAction = request
                )
                return@GladScreen
            }
            GladTabs(listOf("Songs", "Playlists (${playlists.size})"), tab, { tab = it })
            when {
                vm.loading && vm.tracks.isEmpty() -> LoadingBox()
                vm.error != null -> StateMessage(Ic.Warning, "Library unavailable", vm.error.orEmpty(), actionLabel = "Retry", onAction = vm::load)
                tab == 0 -> SongsTab(vm, playlists, player)
                else -> PlaylistsTab(vm, playlists, playlistTracks, onOpenPlaylist)
            }
        }
        if (tab == 1 && granted) {
            FloatingActionButton(
                onClick = { creating = true },
                modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)
            ) { Icon(Ic.Add, "New playlist") }
        }
    }

    if (creating) NameDialog("New playlist", "", { creating = false }) {
        vm.createPlaylist(it)
        creating = false
    }
}

@Composable
private fun SongsTab(vm: MusicViewModel, playlists: List<Playlist>, player: PlayerState) {
    var query by rememberSaveable { mutableStateOf("") }
    val shown = remember(vm.tracks, query) {
        if (query.isBlank()) vm.tracks
        else vm.tracks.filter { it.title.contains(query, true) || it.artist.contains(query, true) || it.album.contains(query, true) }
    }
    OutlinedTextField(
        query, { query = it },
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        placeholder = { Text("Search songs, artists, albums") },
        leadingIcon = { Icon(Ic.Search, null) },
        trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Ic.Close, "Clear") } },
        singleLine = true,
        shape = RoundedCornerShape(50)
    )
    if (shown.isEmpty()) {
        StateMessage(
            Ic.Music,
            if (vm.tracks.isEmpty()) "No music found" else "No matches",
            if (vm.tracks.isEmpty()) "Add audio files to this device, then tap rescan." else "Try a different search."
        )
        return
    }
    LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
        itemsIndexed(shown, key = { _, t -> t.id }) { index, track ->
            TrackRow(
                track = track,
                active = player.mediaId == track.id.toString(),
                playing = player.isPlaying,
                onClick = { vm.play(shown, index) },
                menu = { close ->
                    if (playlists.isEmpty()) {
                        DropdownMenuItem(text = { Text("Create a playlist first") }, onClick = close)
                    } else {
                        playlists.forEach { pl ->
                            DropdownMenuItem(
                                text = { Text("Add to ${pl.name}") },
                                onClick = { vm.addToPlaylist(pl.id, track.id); close() }
                            )
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun TrackRow(
    track: Track,
    active: Boolean,
    playing: Boolean,
    onClick: () -> Unit,
    menu: @Composable (close: () -> Unit) -> Unit
) {
    var open by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(46.dp).clip(RoundedCornerShape(14.dp))
                .background(if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (active && playing) Ic.Pause else if (active) Ic.Play else Ic.Music,
                null,
                tint = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                track.title,
                style = MaterialTheme.typography.titleSmall,
                color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(
                "${track.artist} · ${formatDuration(track.durationMs)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        Box {
            IconButton(onClick = { open = true }) { Icon(Ic.MoreVert, "More") }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) { menu { open = false } }
        }
    }
}

@Composable
private fun PlaylistsTab(
    vm: MusicViewModel,
    playlists: List<Playlist>,
    links: List<PlaylistTrack>,
    onOpen: (Long) -> Unit
) {
    if (playlists.isEmpty()) {
        StateMessage(Ic.Music, "No playlists yet", "Tap + to create your first playlist, then add songs from the Songs tab.")
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(playlists, key = { it.id }) { pl ->
            val count = links.count { it.playlistId == pl.id }
            GladCard(onClick = { onOpen(pl.id) }) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(LocalBrand.current.soft(0.4f)),
                        contentAlignment = Alignment.Center
                    ) { Icon(Ic.Music, null, tint = MaterialTheme.colorScheme.primary) }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(pl.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("$count songs", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Ic.Chevron, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun PlaylistDetailScreen(playlistId: Long, onBack: () -> Unit) {
    val vm = gladViewModel { MusicViewModel(it) }
    val playlists by vm.playlists.collectAsStateWithLifecycle()
    val links by vm.playlistTracks.collectAsStateWithLifecycle()
    val player by vm.player.collectAsStateWithLifecycle()
    val playlist = playlists.firstOrNull { it.id == playlistId }
    var renaming by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val (alreadyGranted, request) = rememberAudioPermission { if (it) vm.load() }

    LaunchedEffect(alreadyGranted) { if (alreadyGranted && !vm.loaded) vm.load() }

    val ids = links.filter { it.playlistId == playlistId }.sortedBy { it.addedAt }.map { it.trackId }
    val byId = remember(vm.tracks) { vm.tracks.associateBy { it.id } }
    val tracks = ids.mapNotNull { byId[it] }

    GladScreen(
        playlist?.name ?: "Playlist",
        subtitle = "${ids.size} songs",
        onBack = onBack,
        actions = {
            IconButton(onClick = { renaming = true }) { Icon(Ic.Edit, "Rename") }
            IconButton(onClick = { confirmDelete = true }) { Icon(Ic.Delete, "Delete playlist") }
        }
    ) {
        when {
            !alreadyGranted && !vm.loaded -> StateMessage(Ic.Music, "Allow access to your music", "Needed to play songs in this playlist.", actionLabel = "Allow access", onAction = request)
            vm.loading -> LoadingBox()
            tracks.isEmpty() -> StateMessage(Ic.Music, "Empty playlist", "Open the Songs tab, tap ⋮ on a song and choose this playlist.")
            else -> LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
                item {
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        FilledIconButton(onClick = { vm.play(tracks, 0) }) { Icon(Ic.Play, "Play all") }
                    }
                }
                itemsIndexed(tracks, key = { _, t -> t.id }) { index, track ->
                    TrackRow(
                        track, player.mediaId == track.id.toString(), player.isPlaying,
                        onClick = { vm.play(tracks, index) },
                        menu = { close ->
                            DropdownMenuItem(
                                text = { Text("Remove from playlist") },
                                onClick = { vm.removeFromPlaylist(playlistId, track.id); close() }
                            )
                        }
                    )
                }
            }
        }
    }

    if (renaming && playlist != null) NameDialog("Rename playlist", playlist.name, { renaming = false }) {
        vm.rename(playlistId, it)
        renaming = false
    }
    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text("Delete playlist?") },
        text = { Text("Your songs stay on the device. Only the playlist is removed.") },
        confirmButton = {
            TextButton(onClick = { vm.deletePlaylist(playlistId); confirmDelete = false; onBack() }) { Text("Delete") }
        },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
    )
}

@Composable
private fun NameDialog(title: String, initial: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { OutlinedTextField(name, { name = it.take(40) }, label = { Text("Name") }, singleLine = true) },
        confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = { onSave(name) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun NowPlayingScreen(onBack: () -> Unit) {
    val music = LocalContext.current.container.music
    val state by music.state.collectAsStateWithLifecycle()
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var dragging by remember { mutableStateOf<Float?>(null) }

    LaunchedEffect(Unit) { music.connect() }
    LaunchedEffect(state.mediaId, state.isPlaying) {
        while (true) {
            if (dragging == null) {
                position = music.positionMs()
                duration = music.durationMs()
            }
            delay(500)
        }
    }

    GladScreen("Now playing", onBack = onBack) {
        if (!state.hasMedia) {
            StateMessage(Ic.Music, "Nothing playing", "Pick a song from your library to start.")
            return@GladScreen
        }
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                Modifier.size(240.dp).clip(RoundedCornerShape(36.dp)).background(LocalBrand.current.brush),
                contentAlignment = Alignment.Center
            ) { Icon(Ic.Music, null, tint = MaterialTheme.colorScheme.background, modifier = Modifier.size(96.dp)) }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(state.title, style = MaterialTheme.typography.headlineSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(state.artist, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            val max = duration.coerceAtLeast(1L).toFloat()
            Slider(
                value = dragging ?: position.toFloat().coerceIn(0f, max),
                onValueChange = { dragging = it },
                onValueChangeFinished = {
                    dragging?.let { music.seekTo(it.toLong()); position = it.toLong() }
                    dragging = null
                },
                valueRange = 0f..max
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatDuration((dragging?.toLong()) ?: position), style = MaterialTheme.typography.labelMedium)
                Text(formatDuration(duration), style = MaterialTheme.typography.labelMedium)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = music::toggleShuffle) {
                    Icon(Ic.Shuffle, "Shuffle", tint = if (state.shuffle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = music::previous) { Icon(Ic.Previous, "Previous", Modifier.size(34.dp)) }
                FilledIconButton(onClick = music::togglePlay, modifier = Modifier.size(72.dp)) {
                    Icon(if (state.isPlaying) Ic.Pause else Ic.Play, if (state.isPlaying) "Pause" else "Play", Modifier.size(38.dp))
                }
                IconButton(onClick = music::next) { Icon(Ic.Next, "Next", Modifier.size(34.dp)) }
                IconButton(onClick = music::cycleRepeat) {
                    Icon(
                        if (state.repeatMode == Player.REPEAT_MODE_ONE) Ic.RepeatOne else Ic.Repeat,
                        "Repeat",
                        tint = if (state.repeatMode != Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/** Compact player pinned above the bottom bar whenever something is loaded. */
@Composable
fun MiniPlayer(state: PlayerState, onOpen: () -> Unit, onToggle: () -> Unit, onNext: () -> Unit, onStop: () -> Unit) {
    var progress by remember { mutableFloatStateOf(0f) }
    val music = LocalContext.current.container.music
    LaunchedEffect(state.mediaId, state.isPlaying) {
        while (true) {
            val d = music.durationMs()
            progress = if (d > 0) (music.positionMs().toFloat() / d).coerceIn(0f, 1f) else 0f
            delay(700)
        }
    }
    GladCard(Modifier.padding(horizontal = 12.dp, vertical = 6.dp).fillMaxWidth(), onClick = onOpen) {
        Column {
            Row(Modifier.padding(start = 14.dp, end = 4.dp, top = 8.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(LocalBrand.current.brush),
                    contentAlignment = Alignment.Center
                ) { Icon(Ic.Music, null, tint = MaterialTheme.colorScheme.background, modifier = Modifier.size(20.dp)) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(state.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(state.artist, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
                IconButton(onClick = onToggle) { Icon(if (state.isPlaying) Ic.Pause else Ic.Play, if (state.isPlaying) "Pause" else "Play") }
                IconButton(onClick = onNext) { Icon(Ic.Next, "Next") }
                IconButton(onClick = onStop) { Icon(Ic.Close, "Stop") }
            }
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(3.dp))
        }
    }
}
