package com.asa.note.repo

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.asa.note.ui.theme.Palette
import com.asa.note.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.System,
    val palette: Palette = Palette.Dynamic,
)

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private val modeKey = stringPreferencesKey("theme_mode")
    private val paletteKey = stringPreferencesKey("palette")

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        AppSettings(
            themeMode = prefs[modeKey].toEnumOrDefault(ThemeMode.System),
            palette = prefs[paletteKey].toEnumOrDefault(Palette.Dynamic),
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { it[modeKey] = mode.name }
    }

    suspend fun setPalette(palette: Palette) {
        context.settingsDataStore.edit { it[paletteKey] = palette.name }
    }
}

/** 存的是枚举名。将来枚举改名或减项时，旧值应当退回默认而不是崩掉。 */
private inline fun <reified T : Enum<T>> String?.toEnumOrDefault(fallback: T): T =
    this?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: fallback
