package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.AppLanguage
import com.example.model.AppSettings
import com.example.model.AppThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("photoshare_prefs", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private fun loadSettings(): AppSettings {
        val autoSync = prefs.getBoolean(KEY_AUTO_SYNC, true)
        val wifiOnly = prefs.getBoolean(KEY_WIFI_ONLY, true)
        val port = prefs.getInt(KEY_PORT, 8080)
        val themeStr = prefs.getString(KEY_THEME, AppThemeMode.SYSTEM.name) ?: AppThemeMode.SYSTEM.name
        val langStr = prefs.getString(KEY_LANG, AppLanguage.EN.name) ?: AppLanguage.EN.name

        val themeMode = try {
            AppThemeMode.valueOf(themeStr)
        } catch (e: Exception) {
            AppThemeMode.SYSTEM
        }

        val language = try {
            AppLanguage.valueOf(langStr)
        } catch (e: Exception) {
            AppLanguage.EN
        }

        return AppSettings(
            autoSync = autoSync,
            wifiOnly = wifiOnly,
            port = port,
            themeMode = themeMode,
            language = language
        )
    }

    fun setAutoSync(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_SYNC, enabled).apply()
        _settings.value = _settings.value.copy(autoSync = enabled)
    }

    fun setWifiOnly(wifiOnly: Boolean) {
        prefs.edit().putBoolean(KEY_WIFI_ONLY, wifiOnly).apply()
        _settings.value = _settings.value.copy(wifiOnly = wifiOnly)
    }

    fun setPort(port: Int) {
        val validPort = if (port in 1024..65535) port else 8080
        prefs.edit().putInt(KEY_PORT, validPort).apply()
        _settings.value = _settings.value.copy(port = validPort)
    }

    fun setThemeMode(themeMode: AppThemeMode) {
        prefs.edit().putString(KEY_THEME, themeMode.name).apply()
        _settings.value = _settings.value.copy(themeMode = themeMode)
    }

    fun setLanguage(language: AppLanguage) {
        prefs.edit().putString(KEY_LANG, language.name).apply()
        _settings.value = _settings.value.copy(language = language)
    }

    companion object {
        private const val KEY_AUTO_SYNC = "key_auto_sync"
        private const val KEY_WIFI_ONLY = "key_wifi_only"
        private const val KEY_PORT = "key_port"
        private const val KEY_THEME = "key_theme"
        private const val KEY_LANG = "key_lang"
    }
}
