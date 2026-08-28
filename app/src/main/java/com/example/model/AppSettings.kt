package com.example.model

enum class AppLanguage(val code: String, val displayName: String, val nativeName: String) {
    EN("en", "English", "English"),
    FA("fa", "Persian", "فارسی")
}

enum class AppThemeMode(val displayNameRes: String) {
    SYSTEM("System Default"),
    LIGHT("Light"),
    DARK("Dark")
}

data class AppSettings(
    val autoSync: Boolean = true,
    val wifiOnly: Boolean = true,
    val port: Int = 8080,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val language: AppLanguage = AppLanguage.EN
)
