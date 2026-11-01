package com.glad.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.glad.app.MainActivity
import com.glad.app.R
import com.glad.app.container
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

internal fun openRouteIntent(context: Context, route: String, requestCode: Int): PendingIntent {
    val intent = Intent(context, MainActivity::class.java)
        .setAction("com.glad.app.OPEN_$route")
        .putExtra(MainActivity.EXTRA_ROUTE, route)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    return PendingIntent.getActivity(
        context,
        requestCode,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}

object WidgetUpdater {
    fun refreshDashboard(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val component = ComponentName(context, DashboardWidgetProvider::class.java)
        val ids = manager.getAppWidgetIds(component)
        if (ids.isEmpty()) return
        context.sendBroadcast(
            Intent(context, DashboardWidgetProvider::class.java)
                .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        )
    }

    fun isPinSupported(context: Context): Boolean =
        AppWidgetManager.getInstance(context).isRequestPinAppWidgetSupported

    fun requestPin(context: Context, provider: Class<*>): Boolean {
        val manager = AppWidgetManager.getInstance(context)
        if (!manager.isRequestPinAppWidgetSupported) return false
        return manager.requestPinAppWidget(ComponentName(context, provider), null, null)
    }
}

/** "Glad Today": clock, date and local watch stats. */
class DashboardWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = context.container.db
                val movies = db.movieDao().countWatchlist()
                val anime = db.animeDao().countWatching()
                ids.forEach { id ->
                    val views = RemoteViews(context.packageName, R.layout.widget_dashboard)
                    views.setTextViewText(R.id.stat_movies, movies.toString())
                    views.setTextViewText(R.id.stat_anime, anime.toString())
                    views.setOnClickPendingIntent(R.id.widget_root, openRouteIntent(context, "home", 100))
                    manager.updateAppWidget(id, views)
                }
            } finally {
                pending.finish()
            }
        }
    }
}

/** "Glad Shortcuts": four one-tap launchers. */
class ShortcutsWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { id ->
            val views = RemoteViews(context.packageName, R.layout.widget_shortcuts)
            views.setOnClickPendingIntent(R.id.sc_wallpapers, openRouteIntent(context, "wallpapers", 201))
            views.setOnClickPendingIntent(R.id.sc_anime, openRouteIntent(context, "anime", 202))
            views.setOnClickPendingIntent(R.id.sc_music, openRouteIntent(context, "music", 203))
            views.setOnClickPendingIntent(R.id.sc_games, openRouteIntent(context, "games", 204))
            manager.updateAppWidget(id, views)
        }
    }
}
