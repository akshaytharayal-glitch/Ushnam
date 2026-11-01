package com.glad.app.ui.anime

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glad.app.AppContainer
import com.glad.app.data.local.AnimeEntry
import com.glad.app.data.local.AnimeStatus
import com.glad.app.data.remote.AnimeSummary
import com.glad.app.data.remote.UiState
import com.glad.app.data.remote.safeApi
import com.glad.app.data.remote.userMessage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AnimeFeed(val label: String) { TOP("Top rated"), AIRING("Airing now") }

class AnimeViewModel(c: AppContainer) : ViewModel() {
    private val api = c.jikan
    private val dao = c.db.animeDao()

    var feed by mutableStateOf(AnimeFeed.TOP)
        private set
    var query by mutableStateOf("")
        private set
    var state by mutableStateOf<UiState<List<AnimeSummary>>>(UiState.Loading)
        private set
    var hasNext by mutableStateOf(false)
        private set
    var loadingMore by mutableStateOf(false)
        private set
    var moreError by mutableStateOf<String?>(null)
        private set

    private var page = 1
    private var job: Job? = null

    val library: StateFlow<List<AnimeEntry>> = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        refresh()
    }

    fun selectFeed(value: AnimeFeed) {
        if (feed == value && query.isBlank()) return
        feed = value
        refresh()
    }

    fun updateQuery(value: String) {
        query = value
        refresh()
    }

    fun refresh() {
        job?.cancel()
        page = 1
        moreError = null
        state = UiState.Loading
        val q = query.trim()
        job = viewModelScope.launch {
            if (q.isNotEmpty()) delay(450)
            safeApi { fetch(q, 1) }
                .onSuccess { p ->
                    hasNext = p.hasNext
                    state = UiState.Success(p.items)
                }
                .onFailure { state = UiState.Error(it.userMessage()) }
        }
    }

    fun loadMore() {
        val current = (state as? UiState.Success)?.data ?: return
        if (loadingMore || !hasNext) return
        loadingMore = true
        moreError = null
        val q = query.trim()
        job = viewModelScope.launch {
            safeApi { fetch(q, page + 1) }
                .onSuccess { p ->
                    page += 1
                    hasNext = p.hasNext
                    state = UiState.Success((current + p.items).distinctBy { it.malId })
                }
                .onFailure { moreError = it.userMessage() }
            loadingMore = false
        }
    }

    private suspend fun fetch(q: String, p: Int) = when {
        q.length >= 2 -> api.search(q, p)
        feed == AnimeFeed.TOP -> api.top(p)
        else -> api.airing(p)
    }
}

class AnimeDetailViewModel(c: AppContainer, private val malId: Int) : ViewModel() {
    private val api = c.jikan
    private val dao = c.db.animeDao()

    var detail by mutableStateOf<UiState<AnimeSummary>>(UiState.Loading)
        private set

    val entry: StateFlow<AnimeEntry?> = dao.observe(malId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        load()
    }

    fun load() {
        detail = UiState.Loading
        viewModelScope.launch {
            safeApi { api.detail(malId) }
                .onSuccess { detail = UiState.Success(it) }
                .onFailure { detail = UiState.Error(it.userMessage()) }
        }
    }

    private fun summary(): AnimeSummary? = (detail as? UiState.Success)?.data

    /** Adds the show to the library, or changes its status when it is already there. */
    fun setStatus(status: String) {
        viewModelScope.launch {
            val existing = entry.value
            val info = summary()
            val total = existing?.totalEpisodes ?: info?.episodes ?: 0
            val watched = when {
                status == AnimeStatus.COMPLETED && total > 0 -> total
                else -> existing?.watched ?: 0
            }
            val base = existing ?: AnimeEntry(
                malId = malId,
                title = info?.title ?: "Anime #$malId",
                imageUrl = info?.imageUrl,
                totalEpisodes = total,
                watched = 0,
                status = status,
                score = 0,
                updatedAt = 0
            )
            dao.upsert(base.copy(status = status, watched = watched, updatedAt = System.currentTimeMillis()))
        }
    }

    fun setProgress(episodes: Int) {
        val current = entry.value ?: return
        viewModelScope.launch {
            val max = if (current.totalEpisodes > 0) current.totalEpisodes else Int.MAX_VALUE
            val value = episodes.coerceIn(0, max)
            val status = when {
                current.totalEpisodes > 0 && value >= current.totalEpisodes -> AnimeStatus.COMPLETED
                value > 0 && (current.status == AnimeStatus.PLAN || current.status == AnimeStatus.COMPLETED) -> AnimeStatus.WATCHING
                else -> current.status
            }
            dao.upsert(current.copy(watched = value, status = status, updatedAt = System.currentTimeMillis()))
        }
    }

    fun setScore(score: Int) {
        val current = entry.value ?: return
        viewModelScope.launch {
            dao.upsert(current.copy(score = score.coerceIn(0, 10), updatedAt = System.currentTimeMillis()))
        }
    }

    fun remove() {
        viewModelScope.launch { dao.delete(malId) }
    }
}
