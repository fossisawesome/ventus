package com.fossisawesome.ventus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fossisawesome.ventus.ui.components.*
import com.fossisawesome.ventus.ui.theme.AppTheme
import com.fossisawesome.ventus.ui.theme.FONT_OPTIONS
import com.fossisawesome.ventus.ui.theme.LocalAppColors
import com.fossisawesome.ventus.ui.theme.LocalAppFontFamily

@Composable
fun SettingsScreen(
    themeId: String,
    fontFamily: String,
    unitsMode: String,
    availableThemes: List<AppTheme>,
    onThemeSelected: (String) -> Unit,
    onFontSelected: (String) -> Unit,
    onUnitsModeSelected: (String) -> Unit,
    onImportTheme: () -> Unit,
    onBack: () -> Unit,
) {
    val colors = LocalAppColors.current
    val font = LocalAppFontFamily.current

    Column(modifier = Modifier.fillMaxSize().background(colors.bg)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                AppIcon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = colors.muted)
            }
            Text("Settings", color = colors.text, fontFamily = font, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Divider()

        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            Text("Theme", color = colors.muted, fontFamily = font, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxWidth().height(240.dp),
            ) {
                items(availableThemes) { theme ->
                    ThemeSwatch(theme = theme, selected = theme.id == themeId, onClick = { onThemeSelected(theme.id) })
                }
            }
            TextButton(onClick = onImportTheme) {
                Text("Import theme…", color = colors.accent, fontFamily = font)
            }

            Spacer(Modifier.height(24.dp))
            Text("Font", color = colors.muted, fontFamily = font, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            FONT_OPTIONS.forEach { option ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onFontSelected(option) }
                        .padding(vertical = 10.dp),
                ) {
                    Text(
                        option,
                        color = if (option == fontFamily) colors.accent else colors.text,
                        fontFamily = font,
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            Text("Units", color = colors.muted, fontFamily = font, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            listOf("auto" to "Auto (by location)", "metric" to "Metric (°C, km/h)", "imperial" to "Imperial (°F, mph)").forEach { (value, label) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onUnitsModeSelected(value) }
                        .padding(vertical = 10.dp),
                ) {
                    Text(label, color = if (value == unitsMode) colors.accent else colors.text, fontFamily = font)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ThemeSwatch(theme: AppTheme, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .padding(6.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(theme.bg)
            .clickable(onClick = onClick)
            .padding(8.dp),
    ) {
        Box(modifier = Modifier.size(24.dp).clip(RoundedCornerShape(4.dp)).background(theme.accent))
        Spacer(Modifier.height(4.dp))
        Text(
            theme.name,
            color = if (selected) theme.accent else theme.text,
            fontSize = 11.sp,
            maxLines = 1,
        )
    }
}
