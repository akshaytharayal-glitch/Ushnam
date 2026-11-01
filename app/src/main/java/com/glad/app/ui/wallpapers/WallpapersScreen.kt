package com.glad.app.ui.wallpapers

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.glad.app.AppContainer
import com.glad.app.data.local.FavoriteWallpaper
import com.glad.app.data.remote.OnlineWallpaper
import com.glad.app.data.remote.PicsumApi
import com.glad.app.data.remote.UiState
import com.glad.app.data.remote.safeApi
import com.glad.app.data.remote.userMessage
import com.glad.app.ui.Ic
import com.glad.app.ui.components.ErrorState
import com.glad.app.ui.components.GladScreen
import com.glad.app.ui.components.GladTabs
import com.glad.app.ui.components.LoadingBox
import com.glad.app.ui.components.RemoteImage
import com.glad.app.ui.components.StateMessage
import com.glad.app.ui.components.gladViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WallpapersViewModel(c: AppContainer) : ViewModel() {
    private val api = c.picsum

    var state by mutableStateOf<UiState<List<OnlineWallpaper>>>(UiState.Loading)
        private set
    var loadingMore by mutableStateOf(false)
        private set
    var hasMore by mutableStateOf(true)
        private set
    var moreError by mutableStateOf<String?>(null)
        private set
    private var page = 1

    val favorites: StateFlow<List<FavoriteWallpaper>> = c.db.wallpaperDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        load()
    }

    fun load() {
        page = 1
        state = UiState.Loading
        viewModelScope.launch {
            safeApi { api.list(page) }
                .onSuccess { state = UiState.Success(it); hasMore = it.isNotEmpty() }
                .onFailure { state = UiState.Error(it.userMessage()) }
        }
    }

    fun loadMore() {
        val current = (state as? UiState.Success)?.data ?: return
        if (loadingMore || !hasMore) return
        loadingMore = true
        moreError = null
        viewModelScope.launch {
            safeApi { api.list(page + 1) }
                .onSuccess { next ->
                    page += 1
                    hasMore = next.isNotEmpty()
                    state = UiState.Success((current + next).distinctBy { it.id })
                }
                .onFailure { moreError = it.userMessage() }
            loadingMore = false
        }
    }
}

@Composable
fun WallpapersScreen(onOpen: (String) -> Unit) {
    val vm = gladViewModel { WallpapersViewModel(it) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val favorites by vm.favorites.collectAsStateWithLifecycle()
    val favoriteKeys = remember(favorites) { favorites.map { it.wallpaperKey }.toSet() }

    GladScreen("Wallpapers", subtitle = "Find something that feels like you") {
        GladTabs(listOf("Discover", "Gradients", "Saved"), tab, { tab = it })
        when (tab) {
            0 -> DiscoverTab(vm, favoriteKeys, onOpen)
            1 -> GradientsTab(favoriteKeys, onOpen)
            else -> SavedTab(favorites, onOpen)
        }
    }
}

private val gridPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp)

@Composable
private fun DiscoverTab(vm: WallpapersViewModel, favoriteKeys: Set<String>, onOpen: (String) -> Unit) {
    when (val s = vm.state) {
        UiState.Loading -> LoadingBox()
        is UiState.Error -> ErrorState(
            s.message + "\n\nThe Gradients tab works fully offline.",
            onRetry = vm::load
        )
        is UiState.Success -> LazyVerticalGrid(
            columns = GridCells.Adaptive(150.dp),
            contentPadding = gridPadding,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(s.data, key = { it.id }) { wp ->
                val item = WallpaperItem.online(wp.id, wp.author)
                WallpaperTile(favorite = item.key in favoriteKeys, onClick = { onOpen(item.key) }) {
                    RemoteImage(PicsumApi.thumbUrl(wp.id), Modifier.fillMaxSize())
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                when {
                    vm.loadingMore -> LoadingBox()
                    vm.moreError != null -> ErrorState(vm.moreError.orEmpty(), onRetry = vm::loadMore)
                    vm.hasMore -> TextButton(onClick = vm::loadMore, modifier = Modifier.fillMaxWidth()) {
                        Text("Load more")
                    }
                    else -> Unit
                }
            }
        }
    }
}

@Composable
private fun GradientsTab(favoriteKeys: Set<String>, onOpen: (String) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(150.dp),
        contentPadding = gridPadding,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        itemsIndexed(GradientCatalog.items) { index, spec ->
            val item = WallpaperItem.gradient(index)
            val image = remember(index) { GradientCatalog.render(spec, 240, 426).asImageBitmap() }
            WallpaperTile(favorite = item.key in favoriteKeys, onClick = { onOpen(item.key) }, label = spec.name) {
                Image(image, spec.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
        }
    }
}

@Composable
private fun SavedTab(favorites: List<FavoriteWallpaper>, onOpen: (String) -> Unit) {
    if (favorites.isEmpty()) {
        StateMessage(
            icon = Ic.FavoriteBorder,
            title = "Nothing saved yet",
            message = "Tap the heart on any wallpaper and it will wait for you here, even offline for gradients."
        )
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(150.dp),
        contentPadding = gridPadding,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(favorites, key = { it.wallpaperKey }) { fav ->
            val item = WallpaperItem.fromKey(fav.wallpaperKey, fav.title) ?: return@items
            WallpaperTile(favorite = true, onClick = { onOpen(item.key) }, label = fav.title) {
                if (item.kind == "g") {
                    val index = item.ref.toIntOrNull() ?: 0
                    val spec = GradientCatalog.items[index.coerceIn(GradientCatalog.items.indices)]
                    val image = remember(index) { GradientCatalog.render(spec, 240, 426).asImageBitmap() }
                    Image(image, spec.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    RemoteImage(PicsumApi.thumbUrl(item.ref), Modifier.fillMaxSize())
                }
            }
        }
    }
}

@Composable
private fun WallpaperTile(
    favorite: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    content: @Composable () -> Unit
) {
    Box(
        modifier
            .fillMaxWidth()
            .aspectRatio(9f / 16f)
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
    ) {
        content()
        if (label != null) {
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.35f))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(label, color = Color.White, style = MaterialTheme.typography.labelLarge, maxLines = 1)
            }
        }
        if (favorite) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Ic.Favorite, null, tint = Color(0xFFFF6B7A), modifier = Modifier.size(18.dp))
            }
        }
    }
}
