package com.fossisawesome.ventus.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.fossisawesome.ventus.MainActivity
import com.fossisawesome.ventus.VentusApplication
import com.fossisawesome.ventus.data.Units
import com.fossisawesome.ventus.data.tempValue
import com.fossisawesome.ventus.data.weatherCodeInfo
import com.fossisawesome.ventus.data.model.Location
import com.fossisawesome.ventus.data.model.WeatherUiState
import com.fossisawesome.ventus.ui.theme.ALL_THEMES
import com.fossisawesome.ventus.ui.theme.AppTheme
import com.fossisawesome.ventus.ui.theme.allThemes
import kotlinx.coroutines.flow.first

class VentusWidget : GlanceAppWidget() {

    companion object {
        private val COMPACT = DpSize(110.dp, 40.dp)
        private val DETAILED = DpSize(250.dp, 110.dp)
    }

    override val sizeMode = SizeMode.Responsive(setOf(COMPACT, DETAILED))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as VentusApplication
        val activeId = app.locationRepository.activeLocationIdFlow.first()
        val locations = app.locationRepository.locationsFlow.first()
        val activeLocation = locations.find { it.id == activeId }
        val state = if (activeLocation != null) {
            app.weatherRepository.loadCached(activeLocation.id)
        } else {
            WeatherUiState.NeedsLocation
        }
        val themeId = app.prefs.themeId.first()
        val theme = allThemes(context).find { it.id == themeId } ?: ALL_THEMES.first()
        val launchIntent = Intent(context, MainActivity::class.java)

        provideContent {
            VentusWidgetContent(activeLocation, state, theme, launchIntent)
        }
    }
}

@Composable
private fun VentusWidgetContent(location: Location?, state: WeatherUiState, theme: AppTheme, launchIntent: Intent) {
    val size = LocalSize.current
    val bgColor = ColorProvider(theme.bg)
    val textColor = ColorProvider(theme.text)
    val mutedColor = ColorProvider(theme.muted)

    val snapshot = when (state) {
        is WeatherUiState.Success -> state.snapshot
        is WeatherUiState.Stale -> state.snapshot
        else -> null
    }

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(bgColor)
            .clickable(actionStartActivity(launchIntent))
            .padding(12.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (location == null || snapshot == null) {
            Text(
                text = "Tap to set up Ventus",
                style = TextStyle(color = textColor, fontSize = 13.sp),
            )
        } else {
            val isImperial = snapshot.units == Units.IMPERIAL
            val info = weatherCodeInfo(snapshot.currentWeatherCode)
            val isDetailed = size.width >= 200.dp

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        provider = ImageProvider(info.icon),
                        contentDescription = info.description,
                        modifier = GlanceModifier.size(24.dp),
                    )
                    Spacer(GlanceModifier.width(6.dp))
                    Text(
                        text = "${tempValue(snapshot.currentTempC, isImperial)}°",
                        style = TextStyle(color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Bold),
                    )
                }
                Text(
                    text = location.name,
                    style = TextStyle(color = mutedColor, fontSize = 12.sp),
                    maxLines = 1,
                )
                if (isDetailed) {
                    val today = snapshot.daily.firstOrNull()
                    if (today != null) {
                        Text(
                            text = "H:${tempValue(today.tempMaxC, isImperial)}° L:${tempValue(today.tempMinC, isImperial)}°",
                            style = TextStyle(color = mutedColor, fontSize = 12.sp),
                        )
                    }
                }
            }
        }
    }
}
