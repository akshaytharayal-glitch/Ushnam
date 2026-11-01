package com.glad.app.ui.nav

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.glad.app.container
import com.glad.app.ui.ai.AiHubScreen
import com.glad.app.ui.ai.AiToolScreen
import com.glad.app.ui.anime.AnimeDetailScreen
import com.glad.app.ui.anime.AnimeScreen
import com.glad.app.ui.appearance.AppearanceScreen
import com.glad.app.ui.games.Game2048Screen
import com.glad.app.ui.games.GameMemoryScreen
import com.glad.app.ui.games.GameTicTacToeScreen
import com.glad.app.ui.games.GamesScreen
import com.glad.app.ui.home.HomeScreen
import com.glad.app.ui.more.MoreScreen
import com.glad.app.ui.movies.MoviesScreen
import com.glad.app.ui.music.MiniPlayer
import com.glad.app.ui.music.MusicScreen
import com.glad.app.ui.music.NowPlayingScreen
import com.glad.app.ui.music.PlaylistDetailScreen
import com.glad.app.ui.wallpapers.WallpaperDetailScreen
import com.glad.app.ui.wallpapers.WallpapersScreen
import com.glad.app.ui.widgets.WidgetsScreen
import kotlinx.coroutines.launch

private val topLevel = BottomTabs.map { it.route }.toSet()
private const val DUR = 280

@Composable
fun GladRoot(requestedRoute: String?, onRouteHandled: () -> Unit) {
    val context = LocalContext.current
    val container = context.container
    val nav = rememberNavController()
    val scope = rememberCoroutineScope()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val player by container.music.state.collectAsStateWithLifecycle()

    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    LaunchedEffect(Unit) { container.music.connect() }

    LaunchedEffect(route) {
        // Media notifications need runtime permission on Android 13+.
        if (route == Routes.MUSIC && Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    fun open(target: String) {
        if (Features.byRoute(target) != null) scope.launch { container.settings.pushRecent(target) }
        nav.navigate(target) { launchSingleTop = true }
    }

    fun switchTab(target: String) {
        nav.navigate(target) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    LaunchedEffect(requestedRoute) {
        val target = requestedRoute ?: return@LaunchedEffect
        if (target in Routes.externalEntry || target == Routes.NOW_PLAYING) {
            if (target == Routes.HOME) switchTab(Routes.HOME) else {
                switchTab(Routes.HOME)
                open(target)
            }
        }
        onRouteHandled()
    }

    val showBottomBar = route in topLevel
    val showMini = player.hasMedia && route != Routes.NOW_PLAYING && route?.startsWith("wallpaper/") != true

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        NavHost(
            navController = nav,
            startDestination = Routes.HOME,
            modifier = Modifier.weight(1f),
            enterTransition = { slideInHorizontally(tween(DUR)) { it / 8 } + fadeIn(tween(DUR)) },
            exitTransition = { fadeOut(tween(DUR / 2)) },
            popEnterTransition = { fadeIn(tween(DUR)) },
            popExitTransition = { slideOutHorizontally(tween(DUR)) { it / 8 } + fadeOut(tween(DUR)) }
        ) {
            composable(Routes.HOME, enterTransition = { fadeIn(tween(DUR)) }) {
                HomeScreen(onOpen = ::open, onOpenAnime = { open(Routes.anime(it)) })
            }
            composable(Routes.WALLPAPERS, enterTransition = { fadeIn(tween(DUR)) }) {
                WallpapersScreen(onOpen = { open(Routes.wallpaper(it)) })
            }
            composable(
                Routes.WALLPAPER_DETAIL,
                arguments = listOf(navArgument("key") { type = NavType.StringType }),
                enterTransition = { slideInVertically(tween(DUR)) { it / 6 } + fadeIn(tween(DUR)) },
                popExitTransition = { slideOutVertically(tween(DUR)) { it / 6 } + fadeOut(tween(DUR)) }
            ) { entry ->
                WallpaperDetailScreen(entry.arguments?.getString("key").orEmpty(), onBack = { nav.popBackStack() })
            }
            composable(Routes.ANIME, enterTransition = { fadeIn(tween(DUR)) }) {
                AnimeScreen(onOpen = { open(Routes.anime(it)) })
            }
            composable(
                Routes.ANIME_DETAIL,
                arguments = listOf(navArgument("id") { type = NavType.IntType })
            ) { entry ->
                AnimeDetailScreen(entry.arguments?.getInt("id") ?: 0, onBack = { nav.popBackStack() })
            }
            composable(Routes.MOVIES, enterTransition = { fadeIn(tween(DUR)) }) { MoviesScreen() }
            composable(Routes.MORE, enterTransition = { fadeIn(tween(DUR)) }) { MoreScreen(onOpen = ::open) }

            composable(Routes.AI) { AiHubScreen(onOpen = ::open, onBack = { nav.popBackStack() }) }
            composable(
                Routes.AI_TOOL,
                arguments = listOf(navArgument("tool") { type = NavType.StringType })
            ) { entry ->
                AiToolScreen(entry.arguments?.getString("tool").orEmpty(), onBack = { nav.popBackStack() })
            }

            composable(Routes.GAMES) { GamesScreen(onOpen = ::open, onBack = { nav.popBackStack() }) }
            composable(Routes.GAME_2048) { Game2048Screen(onBack = { nav.popBackStack() }) }
            composable(Routes.GAME_MEMORY) { GameMemoryScreen(onBack = { nav.popBackStack() }) }
            composable(Routes.GAME_TTT) { GameTicTacToeScreen(onBack = { nav.popBackStack() }) }

            composable(Routes.MUSIC) {
                MusicScreen(
                    onBack = { nav.popBackStack() },
                    onOpenPlaylist = { open(Routes.playlist(it)) }
                )
            }
            composable(
                Routes.PLAYLIST,
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { entry ->
                PlaylistDetailScreen(entry.arguments?.getLong("id") ?: 0L, onBack = { nav.popBackStack() })
            }
            composable(
                Routes.NOW_PLAYING,
                enterTransition = { slideInVertically(tween(DUR)) { it / 4 } + fadeIn(tween(DUR)) },
                popExitTransition = { slideOutVertically(tween(DUR)) { it / 4 } + fadeOut(tween(DUR)) }
            ) { NowPlayingScreen(onBack = { nav.popBackStack() }) }

            composable(Routes.WIDGETS) { WidgetsScreen(onBack = { nav.popBackStack() }) }
            composable(Routes.APPEARANCE) { AppearanceScreen(onBack = { nav.popBackStack() }) }
        }

        AnimatedVisibility(
            visible = showMini,
            enter = slideInVertically(tween(DUR)) { it } + fadeIn(),
            exit = slideOutVertically(tween(DUR)) { it } + fadeOut()
        ) {
            MiniPlayer(
                state = player,
                onOpen = { open(Routes.NOW_PLAYING) },
                onToggle = container.music::togglePlay,
                onNext = container.music::next,
                onStop = container.music::stop
            )
        }

        AnimatedVisibility(
            visible = showBottomBar,
            enter = EnterTransition.None,
            exit = ExitTransition.None
        ) {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                BottomTabs.forEach { tab ->
                    NavigationBarItem(
                        selected = route == tab.route,
                        onClick = { if (route != tab.route) switchTab(tab.route) },
                        icon = { Icon(tab.icon, null) },
                        label = { Text(tab.label) },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }
        if (!showBottomBar) Spacer(Modifier.navigationBarsPadding())
    }
}
