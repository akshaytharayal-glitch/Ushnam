package com.glad.app.ui.nav

import androidx.compose.ui.graphics.vector.ImageVector
import com.glad.app.ui.Ic

object Routes {
    const val HOME = "home"
    const val WALLPAPERS = "wallpapers"
    const val WALLPAPER_DETAIL = "wallpaper/{key}"
    const val ANIME = "anime"
    const val ANIME_DETAIL = "anime/{id}"
    const val MOVIES = "movies"
    const val MORE = "more"
    const val AI = "ai"
    const val AI_TOOL = "ai/{tool}"
    const val GAMES = "games"
    const val GAME_2048 = "games/2048"
    const val GAME_MEMORY = "games/memory"
    const val GAME_TTT = "games/tictactoe"
    const val MUSIC = "music"
    const val NOW_PLAYING = "music/now"
    const val PLAYLIST = "music/playlist/{id}"
    const val WIDGETS = "widgets"
    const val APPEARANCE = "appearance"

    fun wallpaper(key: String) = "wallpaper/$key"
    fun anime(id: Int) = "anime/$id"
    fun aiTool(id: String) = "ai/$id"
    fun playlist(id: Long) = "music/playlist/$id"

    /** Routes opened by home-screen widgets. */
    val externalEntry = setOf(WALLPAPERS, ANIME, MOVIES, AI, GAMES, MUSIC, WIDGETS, APPEARANCE, HOME)
}

data class Feature(val route: String, val title: String, val tagline: String, val icon: ImageVector)

object Features {
    val all = listOf(
        Feature(Routes.WALLPAPERS, "Wallpapers", "Browse, save and apply", Ic.Wallpaper),
        Feature(Routes.ANIME, "Anime", "Discover and track episodes", Ic.Tv),
        Feature(Routes.MOVIES, "Movies", "Watchlist, history, ratings", Ic.Movie),
        Feature(Routes.AI, "AI Hub", "Smart writing and text tools", Ic.Sparkle),
        Feature(Routes.GAMES, "Games", "Three offline mini-games", Ic.Games),
        Feature(Routes.MUSIC, "Music", "Your local library and playlists", Ic.Music),
        Feature(Routes.WIDGETS, "Widgets", "Home-screen helpers", Ic.Widgets),
        Feature(Routes.APPEARANCE, "Appearance", "Themes and accent colours", Ic.Palette)
    )

    /** Features that live under the "More" tab. */
    val more = all.filter { it.route in setOf(Routes.AI, Routes.GAMES, Routes.MUSIC, Routes.WIDGETS, Routes.APPEARANCE) }

    fun byRoute(route: String) = all.firstOrNull { it.route == route }
}

data class BottomTab(val route: String, val label: String, val icon: ImageVector)

val BottomTabs = listOf(
    BottomTab(Routes.HOME, "Home", Ic.Home),
    BottomTab(Routes.WALLPAPERS, "Walls", Ic.Wallpaper),
    BottomTab(Routes.ANIME, "Anime", Ic.Tv),
    BottomTab(Routes.MOVIES, "Movies", Ic.Movie),
    BottomTab(Routes.MORE, "More", Ic.More)
)
