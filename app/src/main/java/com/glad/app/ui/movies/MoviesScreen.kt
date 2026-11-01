package com.glad.app.ui.movies

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.glad.app.AppContainer
import com.glad.app.data.local.MovieEntry
import com.glad.app.data.local.MovieStatus
import com.glad.app.ui.Ic
import com.glad.app.ui.components.GladCard
import com.glad.app.ui.components.GladScreen
import com.glad.app.ui.components.GladTabs
import com.glad.app.ui.components.StateMessage
import com.glad.app.ui.components.gladViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

class MoviesViewModel(c: AppContainer) : ViewModel() {
    private val dao = c.db.movieDao()

    val movies: StateFlow<List<MovieEntry>> = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun add(title: String, year: Int?, genre: String, notes: String) {
        viewModelScope.launch {
            dao.upsert(
                MovieEntry(
                    title = title.trim(), year = year, genre = genre.trim(), notes = notes.trim(),
                    status = MovieStatus.WATCHLIST, rating = 0,
                    addedAt = System.currentTimeMillis(), watchedAt = null
                )
            )
        }
    }

    fun update(movie: MovieEntry) {
        viewModelScope.launch { dao.upsert(movie) }
    }

    fun markWatched(movie: MovieEntry, rating: Int) {
        viewModelScope.launch {
            dao.upsert(movie.copy(status = MovieStatus.WATCHED, rating = rating, watchedAt = System.currentTimeMillis()))
        }
    }

    fun moveToWatchlist(movie: MovieEntry) {
        viewModelScope.launch {
            dao.upsert(movie.copy(status = MovieStatus.WATCHLIST, watchedAt = null))
        }
    }

    fun delete(movie: MovieEntry) {
        viewModelScope.launch { dao.delete(movie.id) }
    }
}

private enum class MovieSort(val label: String) { RECENT("Recent"), TITLE("A–Z"), RATING("Rating") }

@Composable
fun MoviesScreen() {
    val vm = gladViewModel { MoviesViewModel(it) }
    val movies by vm.movies.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    var sort by rememberSaveable { mutableStateOf(MovieSort.RECENT.name) }
    var editing by remember { mutableStateOf<MovieEntry?>(null) }
    var adding by remember { mutableStateOf(false) }
    var rating by remember { mutableStateOf<MovieEntry?>(null) }

    val watchlist = movies.count { it.status == MovieStatus.WATCHLIST }
    val watched = movies.filter { it.status == MovieStatus.WATCHED }
    val average = watched.filter { it.rating > 0 }.map { it.rating }.average().takeIf { !it.isNaN() }

    val status = if (tab == 0) MovieStatus.WATCHLIST else MovieStatus.WATCHED
    val visible = remember(movies, tab, query, sort) {
        val base = movies.filter { it.status == status && (query.isBlank() || it.title.contains(query.trim(), true) || it.genre.contains(query.trim(), true)) }
        when (MovieSort.valueOf(sort)) {
            MovieSort.RECENT -> if (status == MovieStatus.WATCHED) base.sortedByDescending { it.watchedAt ?: 0L } else base
            MovieSort.TITLE -> base.sortedBy { it.title.lowercase() }
            MovieSort.RATING -> base.sortedByDescending { it.rating }
        }
    }

    Box(Modifier.fillMaxSize()) {
        GladScreen(
            "Movies",
            subtitle = buildString {
                append("$watchlist to watch · ${watched.size} watched")
                if (average != null) append(" · avg ★ %.1f".format(average))
            }
        ) {
            GladTabs(listOf("Watchlist", "Watched"), tab, { tab = it })
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search title or genre") },
                leadingIcon = { Icon(Ic.Search, null) },
                trailingIcon = {
                    if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Ic.Close, "Clear") }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.extraLarge
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(MovieSort.entries.toList()) { s ->
                    FilterChip(selected = sort == s.name, onClick = { sort = s.name }, label = { Text(s.label) })
                }
            }

            if (visible.isEmpty()) {
                StateMessage(
                    icon = Ic.Movie,
                    title = when {
                        query.isNotBlank() -> "No matches"
                        tab == 0 -> "Your watchlist is empty"
                        else -> "Nothing watched yet"
                    },
                    message = when {
                        query.isNotBlank() -> "Try another title or genre."
                        tab == 0 -> "Add the films you want to see. Everything is stored on this device."
                        else -> "Mark a movie as watched and rate it to build your history."
                    }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(visible, key = { it.id }) { movie ->
                        MovieRow(
                            movie,
                            onWatched = { rating = movie },
                            onBack = { vm.moveToWatchlist(movie) },
                            onEdit = { editing = movie },
                            onDelete = { vm.delete(movie) }
                        )
                    }
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = { adding = true },
            icon = { Icon(Ic.Add, null) },
            text = { Text("Add movie") },
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)
        )
    }

    if (adding) {
        MovieDialog(initial = null, onDismiss = { adding = false }) { title, year, genre, notes ->
            vm.add(title, year, genre, notes)
            adding = false
        }
    }
    editing?.let { movie ->
        MovieDialog(initial = movie, onDismiss = { editing = null }) { title, year, genre, notes ->
            vm.update(movie.copy(title = title.trim(), year = year, genre = genre.trim(), notes = notes.trim()))
            editing = null
        }
    }
    rating?.let { movie ->
        RatingDialog(
            title = movie.title,
            initial = movie.rating,
            onDismiss = { rating = null },
            onSave = { stars ->
                vm.markWatched(movie, stars)
                rating = null
            }
        )
    }
}

@Composable
private fun MovieRow(
    movie: MovieEntry,
    onWatched: () -> Unit,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    GladCard(onClick = { expanded = !expanded }) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(movie.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    val meta = listOfNotNull(movie.year?.toString(), movie.genre.takeIf { it.isNotBlank() }).joinToString(" · ")
                    if (meta.isNotEmpty()) {
                        Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (movie.status == MovieStatus.WATCHED) Stars(movie.rating)
            }
            if (movie.status == MovieStatus.WATCHED && movie.watchedAt != null) {
                Text(
                    "Watched " + DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(movie.watchedAt)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            if (expanded) {
                if (movie.notes.isNotBlank()) {
                    Text(
                        movie.notes,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (movie.status == MovieStatus.WATCHLIST) {
                        Button(onClick = onWatched) {
                            Icon(Ic.Check, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Watched")
                        }
                    } else {
                        TextButton(onClick = onWatched) { Text("Re-rate") }
                        TextButton(onClick = onBack) { Text("Back to list") }
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onEdit) { Icon(Ic.Edit, "Edit") }
                    IconButton(onClick = onDelete) { Icon(Ic.Delete, "Delete") }
                }
            }
        }
    }
}

@Composable
private fun Stars(rating: Int) {
    Row {
        repeat(5) { i ->
            Icon(
                if (i < rating) Ic.Star else Ic.StarBorder,
                null,
                tint = androidx.compose.ui.graphics.Color(0xFFFFC145),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun RatingDialog(title: String, initial: Int, onDismiss: () -> Unit, onSave: (Int) -> Unit) {
    var stars by remember { mutableIntStateOf(if (initial > 0) initial else 4) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rate \"$title\"") },
        text = {
            Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                repeat(5) { i ->
                    IconButton(onClick = { stars = i + 1 }) {
                        Icon(
                            if (i < stars) Ic.Star else Ic.StarBorder,
                            "${i + 1} stars",
                            tint = androidx.compose.ui.graphics.Color(0xFFFFC145),
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(stars) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun MovieDialog(
    initial: MovieEntry?,
    onDismiss: () -> Unit,
    onSave: (title: String, year: Int?, genre: String, notes: String) -> Unit
) {
    var title by remember { mutableStateOf(initial?.title.orEmpty()) }
    var year by remember { mutableStateOf(initial?.year?.toString().orEmpty()) }
    var genre by remember { mutableStateOf(initial?.genre.orEmpty()) }
    var notes by remember { mutableStateOf(initial?.notes.orEmpty()) }
    val yearValue = year.toIntOrNull()
    val yearOk = year.isBlank() || (yearValue != null && yearValue in 1888..2100)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add movie" else "Edit movie") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Title") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        year,
                        { year = it.filter(Char::isDigit).take(4) },
                        label = { Text("Year") },
                        singleLine = true,
                        isError = !yearOk,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        genre,
                        { genre = it },
                        label = { Text("Genre") },
                        singleLine = true,
                        modifier = Modifier.weight(1.4f)
                    )
                }
                OutlinedTextField(notes, { notes = it }, label = { Text("Notes (optional)") }, minLines = 2, maxLines = 4)
            }
        },
        confirmButton = {
            TextButton(
                enabled = title.isNotBlank() && yearOk,
                onClick = { onSave(title, yearValue, genre, notes) }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
