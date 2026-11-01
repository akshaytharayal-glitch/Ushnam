package com.glad.app

import android.content.Context
import androidx.room.Room
import com.glad.app.data.local.GladDatabase
import com.glad.app.data.prefs.SettingsStore
import com.glad.app.data.remote.AiClient
import com.glad.app.data.remote.JikanApi
import com.glad.app.data.remote.PicsumApi
import com.glad.app.music.AudioRepository
import com.glad.app.music.MusicController
import com.glad.app.widget.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/** Hand-rolled service locator; keeps the app free of a DI framework. */
@OptIn(FlowPreview::class)
class AppContainer(private val context: Context) {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val db: GladDatabase = Room.databaseBuilder(context, GladDatabase::class.java, "glad.db")
        .fallbackToDestructiveMigration()
        .build()
    val settings = SettingsStore(context)
    val jikan = JikanApi()
    val picsum = PicsumApi()
    val ai = AiClient()
    val audio = AudioRepository(context)
    val music = MusicController(context)

    init {
        // Keep the home-screen dashboard widget in sync with local data.
        appScope.launch {
            combine(
                db.movieDao().observeWatchlistCount(),
                db.animeDao().observeWatchingCount()
            ) { m, a -> m to a }
                .distinctUntilChanged()
                .debounce(400)
                .collect { WidgetUpdater.refreshDashboard(context) }
        }
    }
}
