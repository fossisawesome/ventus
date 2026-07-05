package com.fossisawesome.ventus.ui.theme

import android.content.Context
import android.net.Uri
import androidx.compose.ui.graphics.Color
import java.io.File

// Imported themes live as one .toml per theme under filesDir/themes/. The format
// matches the desktop/Firmium theme files (name, color_scheme, [colors] table), so a
// file authored for either app imports unchanged here.

private const val THEMES_DIR = "themes"
private const val MAX_THEME_BYTES = 50 * 1024

internal data class ParsedTheme(
    val name: String,
    val colorScheme: String?,
    val colors: Map<String, String>,
)

private fun themesDir(context: Context): File = File(context.filesDir, THEMES_DIR)

private fun parseThemeToml(text: String): ParsedTheme? {
    var name: String? = null
    var colorScheme: String? = null
    val colors = mutableMapOf<String, String>()
    var inColors = false

    for (raw in text.lines()) {
        // Strip a trailing "# comment", but only when the '#' falls outside a quoted value —
        // hex colors like "#101010" contain '#' themselves and must not be truncated.
        val withoutComment = if (raw.contains('"')) {
            val lastQuote = raw.lastIndexOf('"')
            val hashAfterQuote = raw.indexOf('#', lastQuote)
            if (hashAfterQuote >= 0) raw.substring(0, hashAfterQuote) else raw
        } else {
            raw.substringBefore('#')
        }
        val line = withoutComment.trim()
        if (line.isEmpty()) continue

        if (line.startsWith("[") && line.endsWith("]")) {
            inColors = line.substring(1, line.length - 1).trim() == "colors"
            continue
        }

        val eq = line.indexOf('=')
        if (eq <= 0) continue
        val key = line.substring(0, eq).trim()
        val value = line.substring(eq + 1).trim().trim('"')

        if (inColors) {
            colors[key] = value
        } else when (key) {
            "name" -> name = value
            "color_scheme" -> colorScheme = value
        }
    }

    val n = name?.trim().orEmpty()
    if (n.isEmpty()) return null
    return ParsedTheme(n, colorScheme, colors)
}

private fun toAppTheme(id: String, parsed: ParsedTheme, sourceFile: String): AppTheme? {
    fun color(key: String): Color? = parsed.colors[key]?.let {
        runCatching { hex(it) }.getOrNull()
    }
    return AppTheme(
        id = id,
        name = parsed.name,
        isDark = parsed.colorScheme != "light",
        bg = color("bg") ?: return null,
        surface = color("surface") ?: return null,
        surface2 = color("surface2") ?: return null,
        text = color("text") ?: return null,
        muted = color("muted") ?: return null,
        accent = color("accent") ?: return null,
        error = color("error") ?: return null,
        isImported = true,
        sourceFile = sourceFile,
    )
}

fun loadImportedThemes(context: Context): List<AppTheme> {
    val dir = themesDir(context)
    val files = dir.listFiles { f -> f.isFile && f.name.endsWith(".toml") } ?: return emptyList()
    return files.mapNotNull { file ->
        val content = runCatching { file.readText() }.getOrNull() ?: return@mapNotNull null
        val parsed = parseThemeToml(content) ?: return@mapNotNull null
        toAppTheme(id = file.nameWithoutExtension, parsed = parsed, sourceFile = file.name)
    }.sortedBy { it.name.lowercase() }
}

fun allThemes(context: Context): List<AppTheme> = ALL_THEMES + loadImportedThemes(context)

private fun sanitizeName(name: String): String {
    val cleaned = name.lowercase().map { c -> if (c.isLetterOrDigit()) c else '-' }.joinToString("")
        .trim('-').replace(Regex("-+"), "-")
    return cleaned.ifEmpty { "theme" }
}

fun importThemeFromUri(context: Context, uri: Uri): Result<Unit> {
    val bytes = runCatching {
        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
    }.getOrNull() ?: return Result.failure(Exception("Couldn't read the selected file"))

    if (bytes.size > MAX_THEME_BYTES) {
        return Result.failure(Exception("File is too large (max 50 KB)"))
    }

    val text = runCatching { bytes.toString(Charsets.UTF_8) }.getOrNull()
        ?: return Result.failure(Exception("File isn't valid text"))
    val parsed = parseThemeToml(text)
        ?: return Result.failure(Exception("Not a valid theme file (missing name or colors)"))
    if (toAppTheme(sanitizeName(parsed.name), parsed, "tmp") == null) {
        return Result.failure(Exception("Theme is missing one or more required colors"))
    }

    val dir = themesDir(context)
    if (!dir.exists() && !dir.mkdirs()) {
        return Result.failure(Exception("Couldn't create the themes folder"))
    }
    val target = File(dir, "${sanitizeName(parsed.name)}.toml")
    return runCatching { target.writeText(text); Unit }
        .recoverCatching { throw Exception("Couldn't save the theme") }
}

fun deleteImportedTheme(context: Context, filename: String) {
    runCatching { File(themesDir(context), filename).delete() }
}

// Exposed for the unit test (Robolectric-free — tests the pure parser only).
internal fun parseThemeTomlForTest(text: String) = parseThemeToml(text)
