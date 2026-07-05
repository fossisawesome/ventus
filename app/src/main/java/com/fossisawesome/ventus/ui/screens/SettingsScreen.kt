package com.fossisawesome.ventus.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
    weatherProvider: String,
    isNwsAvailable: Boolean,
    availableThemes: List<AppTheme>,
    onThemeSelected: (String) -> Unit,
    onFontSelected: (String) -> Unit,
    onUnitsModeSelected: (String) -> Unit,
    onWeatherProviderSelected: (String) -> Unit,
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
            ThemeDropdown(themeId = themeId, availableThemes = availableThemes, onThemeSelected = onThemeSelected)
            TextButton(onClick = onImportTheme) {
                Text("Import theme…", color = colors.accent, fontFamily = font)
            }

            Spacer(Modifier.height(24.dp))
            Text("Font", color = colors.muted, fontFamily = font, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            FontDropdown(fontFamily = fontFamily, onFontSelected = onFontSelected)

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
            Text("Weather Provider", color = colors.muted, fontFamily = font, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            listOf("open-meteo" to "Open-Meteo", "nws" to "NWS (US only)").forEach { (value, label) ->
                val enabled = value != "nws" || isNwsAvailable
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = enabled) { onWeatherProviderSelected(value) }
                        .padding(vertical = 10.dp),
                ) {
                    Text(
                        label,
                        color = when {
                            !enabled -> colors.muted.copy(alpha = 0.5f)
                            value == weatherProvider -> colors.accent
                            else -> colors.text
                        },
                        fontFamily = font,
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ThemeDropdown(themeId: String, availableThemes: List<AppTheme>, onThemeSelected: (String) -> Unit) {
    val colors = LocalAppColors.current
    val font = LocalAppFontFamily.current
    val current = availableThemes.find { it.id == themeId } ?: availableThemes.first()
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .border(1.dp, colors.border, RoundedCornerShape(6.dp))
                .clickable { expanded = !expanded }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(Modifier.size(12.dp).clip(CircleShape).background(current.bg).border(0.5.dp, colors.border, CircleShape))
                Box(Modifier.size(12.dp).clip(CircleShape).background(current.surface2))
                Box(Modifier.size(12.dp).clip(CircleShape).background(current.text).border(0.5.dp, colors.border, CircleShape))
                Box(Modifier.size(12.dp).clip(CircleShape).background(current.accent))
            }
            Text(current.name, color = colors.text, fontFamily = font, fontSize = 14.sp, modifier = Modifier.weight(1f))
            AppIcon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = colors.muted,
                modifier = Modifier.size(18.dp),
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp)
                    .verticalScroll(rememberScrollState())
                    .clip(RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp))
                    .border(1.dp, colors.border, RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp))
                    .background(colors.surface),
            ) {
                availableThemes.forEachIndexed { i, theme ->
                    if (i > 0) Divider(color = colors.border)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onThemeSelected(theme.id); expanded = false }
                            .background(if (theme.id == themeId) colors.surface2.copy(alpha = 0.5f) else Color.Transparent)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Box(Modifier.size(12.dp).clip(CircleShape).background(theme.bg).border(0.5.dp, colors.border, CircleShape))
                            Box(Modifier.size(12.dp).clip(CircleShape).background(theme.surface2))
                            Box(Modifier.size(12.dp).clip(CircleShape).background(theme.text).border(0.5.dp, colors.border, CircleShape))
                            Box(Modifier.size(12.dp).clip(CircleShape).background(theme.accent))
                        }
                        Text(
                            theme.name,
                            fontFamily = font,
                            fontSize = 13.sp,
                            color = if (theme.id == themeId) colors.accent else colors.text,
                            modifier = Modifier.weight(1f),
                        )
                        if (theme.id == themeId) {
                            AppIcon(Icons.Default.Check, contentDescription = null, tint = colors.accent, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FontDropdown(fontFamily: String, onFontSelected: (String) -> Unit) {
    val colors = LocalAppColors.current
    val font = LocalAppFontFamily.current
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .border(1.dp, colors.border, RoundedCornerShape(6.dp))
                .clickable { expanded = !expanded }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(fontFamily, color = colors.text, fontFamily = font, fontSize = 14.sp, modifier = Modifier.weight(1f))
            AppIcon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = colors.muted,
                modifier = Modifier.size(18.dp),
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp)
                    .verticalScroll(rememberScrollState())
                    .clip(RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp))
                    .border(1.dp, colors.border, RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp))
                    .background(colors.surface),
            ) {
                FONT_OPTIONS.forEachIndexed { i, option ->
                    if (i > 0) Divider(color = colors.border)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onFontSelected(option); expanded = false }
                            .background(if (option == fontFamily) colors.surface2.copy(alpha = 0.5f) else Color.Transparent)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            option,
                            fontFamily = font,
                            fontSize = 13.sp,
                            color = if (option == fontFamily) colors.accent else colors.text,
                            modifier = Modifier.weight(1f),
                        )
                        if (option == fontFamily) {
                            AppIcon(Icons.Default.Check, contentDescription = null, tint = colors.accent, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }
    }
}
