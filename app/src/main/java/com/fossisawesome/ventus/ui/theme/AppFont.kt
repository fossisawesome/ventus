package com.fossisawesome.ventus.ui.theme

// Maps the user-facing font display names (Settings > Appearance) to a key.
enum class AppFontKey {
    INTER, LIBERATION_MONO, MONOSPACE, DEFAULT, SANS_SERIF,
    BIGBLUE_TERMINAL, COUSINE, FIRACODE, HACK,
}

val FONT_OPTIONS: List<String> = listOf(
    "Inter", "Liberation Mono", "Monospace", "System",
    "Sans Serif", "BigBlue Terminal", "Cousine", "FiraCode", "Hack",
)

fun fontKeyFor(displayName: String): AppFontKey = when (displayName) {
    "Inter" -> AppFontKey.INTER
    "Liberation Mono" -> AppFontKey.LIBERATION_MONO
    "Monospace" -> AppFontKey.MONOSPACE
    "Sans Serif" -> AppFontKey.SANS_SERIF
    "BigBlue Terminal" -> AppFontKey.BIGBLUE_TERMINAL
    "Cousine" -> AppFontKey.COUSINE
    "FiraCode" -> AppFontKey.FIRACODE
    "Hack" -> AppFontKey.HACK
    else -> AppFontKey.DEFAULT // "System" and any unrecognized value
}
