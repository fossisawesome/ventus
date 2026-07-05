package com.fossisawesome.ventus.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ThemeImportTest {

    @Test
    fun `parses a well-formed theme file`() {
        val toml = """
            name = "Custom Storm"
            color_scheme = "dark"

            [colors]
            bg = "#101010"
            surface = "#202020"
            surface2 = "#303030"
            text = "#f0f0f0"
            muted = "#888888"
            accent = "#66ccff"
            error = "#ff5555"
        """.trimIndent()

        val parsed = parseThemeTomlForTest(toml)
        assertEquals("Custom Storm", parsed?.name)
        assertEquals("dark", parsed?.colorScheme)
        assertEquals("#101010", parsed?.colors?.get("bg"))
        assertEquals("#66ccff", parsed?.colors?.get("accent"))
    }

    @Test
    fun `returns null when name is missing`() {
        val toml = """
            color_scheme = "dark"
            [colors]
            bg = "#101010"
        """.trimIndent()

        assertNull(parseThemeTomlForTest(toml))
    }

    @Test
    fun `ignores comments and blank lines`() {
        val toml = """
            # this is a comment
            name = "Commented"

            [colors]
            # another comment
            bg = "#111111" # trailing comment
        """.trimIndent()

        val parsed = parseThemeTomlForTest(toml)
        assertEquals("Commented", parsed?.name)
        assertEquals("#111111", parsed?.colors?.get("bg"))
    }
}
