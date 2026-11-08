package com.marblemd.app.model

/** App appearance, chosen in Settings. SYSTEM follows the device setting. */
enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark")
}
