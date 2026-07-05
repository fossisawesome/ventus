package com.fossisawesome.ventus.ui.theme

// Maps the user-facing font display names (Settings > Appearance) to a key.
enum class AppFontKey {
    INTER, LIBERATION_MONO, MONOSPACE, DEFAULT,
    BIGBLUE_TERMINAL, COUSINE, FIRACODE, HACK,
}

val FONT_OPTIONS: List<String> = listOf(
    "Inter", "Liberation Mono", "Monospace", "System",
    "BigBlue Terminal", "Cousine", "FiraCode", "Hack",
)

fun fontKeyFor(displayName: String): AppFontKey = when (displayName) {
    "Inter" -> AppFontKey.INTER
    "Liberation Mono" -> AppFontKey.LIBERATION_MONO
    "Monospace" -> AppFontKey.MONOSPACE
    "BigBlue Terminal" -> AppFontKey.BIGBLUE_TERMINAL
    "Cousine" -> AppFontKey.COUSINE
    "FiraCode" -> AppFontKey.FIRACODE
    "Hack" -> AppFontKey.HACK
    else -> AppFontKey.DEFAULT // "System" and any unrecognized value
}
