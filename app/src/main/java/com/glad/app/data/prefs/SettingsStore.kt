package com.glad.app.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.gladDataStore: DataStore<Preferences> by preferencesDataStore(name = "glad_settings")

enum class ThemeMode { SYSTEM, DARK, LIGHT }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val accentIndex: Int = 0,
    val amoled: Boolean = false,
    val dynamicColor: Boolean = false
)

/** AI provider configuration. The key is entered by the user and stays on this device. */
data class AiConfig(
    val useCustom: Boolean = false,
    val endpoint: String = "",
    val apiKey: String = "",
    val model: String = ""
)

data class GameStats(
    val best2048: Int = 0,
    val bestMemoryMoves: Int = 0,
    val tttWins: Int = 0,
    val tttDraws: Int = 0,
    val tttLosses: Int = 0
)

class SettingsStore(private val context: Context) {
    private object K {
        val THEME = stringPreferencesKey("theme_mode")
        val ACCENT = intPreferencesKey("accent_index")
        val AMOLED = booleanPreferencesKey("amoled")
        val DYNAMIC = booleanPreferencesKey("dynamic_color")
        val RECENTS = stringPreferencesKey("recents")
        val AI_CUSTOM = booleanPreferencesKey("ai_custom")
        val AI_ENDPOINT = stringPreferencesKey("ai_endpoint")
        val AI_KEY = stringPreferencesKey("ai_key")
        val AI_MODEL = stringPreferencesKey("ai_model")
        val BEST_2048 = intPreferencesKey("best_2048")
        val BEST_MEMORY = intPreferencesKey("best_memory")
        val TTT_W = intPreferencesKey("ttt_w")
        val TTT_D = intPreferencesKey("ttt_d")
        val TTT_L = intPreferencesKey("ttt_l")
    }

    private val data: Flow<Preferences>
        get() = context.gladDataStore.data.catch { emit(emptyPreferences()) }

    val settings: Flow<AppSettings> = data.map { p ->
        AppSettings(
            themeMode = p[K.THEME]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.DARK,
            accentIndex = p[K.ACCENT] ?: 0,
            amoled = p[K.AMOLED] ?: false,
            dynamicColor = p[K.DYNAMIC] ?: false
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) = context.gladDataStore.edit { it[K.THEME] = mode.name }
    suspend fun setAccent(index: Int) = context.gladDataStore.edit { it[K.ACCENT] = index }
    suspend fun setAmoled(on: Boolean) = context.gladDataStore.edit { it[K.AMOLED] = on }
    suspend fun setDynamicColor(on: Boolean) = context.gladDataStore.edit { it[K.DYNAMIC] = on }

    val recents: Flow<List<String>> = data.map { p ->
        p[K.RECENTS]?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
    }

    suspend fun pushRecent(route: String) {
        context.gladDataStore.edit { p ->
            val current = p[K.RECENTS]?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
            p[K.RECENTS] = (listOf(route) + current.filter { it != route }).take(6).joinToString(",")
        }
    }

    val aiConfig: Flow<AiConfig> = data.map { p ->
        AiConfig(
            useCustom = p[K.AI_CUSTOM] ?: false,
            endpoint = p[K.AI_ENDPOINT].orEmpty(),
            apiKey = p[K.AI_KEY].orEmpty(),
            model = p[K.AI_MODEL].orEmpty()
        )
    }

    suspend fun setAiConfig(config: AiConfig) {
        context.gladDataStore.edit { p ->
            p[K.AI_CUSTOM] = config.useCustom
            p[K.AI_ENDPOINT] = config.endpoint.trim()
            p[K.AI_KEY] = config.apiKey.trim()
            p[K.AI_MODEL] = config.model.trim()
        }
    }

    val gameStats: Flow<GameStats> = data.map { p ->
        GameStats(
            best2048 = p[K.BEST_2048] ?: 0,
            bestMemoryMoves = p[K.BEST_MEMORY] ?: 0,
            tttWins = p[K.TTT_W] ?: 0,
            tttDraws = p[K.TTT_D] ?: 0,
            tttLosses = p[K.TTT_L] ?: 0
        )
    }

    suspend fun report2048(score: Int) {
        context.gladDataStore.edit { p -> if (score > (p[K.BEST_2048] ?: 0)) p[K.BEST_2048] = score }
    }

    suspend fun reportMemory(moves: Int) {
        context.gladDataStore.edit { p ->
            val best = p[K.BEST_MEMORY] ?: 0
            if (best == 0 || moves < best) p[K.BEST_MEMORY] = moves
        }
    }

    /** result: 1 = player won, 0 = draw, -1 = player lost. */
    suspend fun reportTicTacToe(result: Int) {
        context.gladDataStore.edit { p ->
            when (result) {
                1 -> p[K.TTT_W] = (p[K.TTT_W] ?: 0) + 1
                0 -> p[K.TTT_D] = (p[K.TTT_D] ?: 0) + 1
                else -> p[K.TTT_L] = (p[K.TTT_L] ?: 0) + 1
            }
        }
    }
}
