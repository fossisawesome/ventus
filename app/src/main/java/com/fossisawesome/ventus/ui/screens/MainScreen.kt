package com.fossisawesome.ventus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fossisawesome.ventus.R
import com.fossisawesome.ventus.data.*
import com.fossisawesome.ventus.data.model.*
import com.fossisawesome.ventus.ui.components.*
import com.fossisawesome.ventus.ui.theme.LocalAppColors
import com.fossisawesome.ventus.ui.theme.LocalAppFontFamily
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(androidx.compose.material.ExperimentalMaterialApi::class)
@Composable
fun MainScreen(
    state: WeatherUiState,
    searchResults: List<GeocodingResult>,
    onRefresh: () -> Unit,
    onUseCurrentLocation: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSelectSearchResult: (GeocodingResult) -> Unit,
    onSettingsClick: () -> Unit,
) {
    val colors = LocalAppColors.current
    val font = LocalAppFontFamily.current
    var query by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }
    val isRefreshing = state is WeatherUiState.Loading
    val pullState = rememberPullRefreshState(refreshing = isRefreshing, onRefresh = onRefresh)

    Box(modifier = Modifier.fillMaxSize().background(colors.bg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(20.dp, 20.dp, 20.dp, 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { showSearch = !showSearch }, modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        AppIcon(R.drawable.ic_pin, null, tint = colors.accent, modifier = Modifier.size(20.dp))
                        Text(
                            text = locationTitle(state),
                            color = colors.text,
                            fontFamily = font,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.2).sp,
                        )
                        AppIcon(R.drawable.ic_chevron_right, null, tint = colors.muted, modifier = Modifier.size(16.dp))
                    }
                }
                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.accent.copy(alpha = 0.08f)),
                ) {
                    AppIcon(R.drawable.ic_settings, "Settings", tint = colors.accent, modifier = Modifier.size(18.dp))
                }
            }

            Box(modifier = Modifier.weight(1f).pullRefresh(pullState)) {
                when (state) {
                    is WeatherUiState.Loading -> LoadingBody()
                    is WeatherUiState.NeedsLocation -> NeedsLocationBody(onUseCurrentLocation)
                    is WeatherUiState.Error -> ErrorBody(state.message, onRefresh)
                    is WeatherUiState.Success -> ForecastBody(state.snapshot, staleBanner = null)
                    is WeatherUiState.Stale -> ForecastBody(state.snapshot, staleBanner = staleLabel(state.fetchedAt))
                }
                PullRefreshIndicator(
                    refreshing = isRefreshing,
                    state = pullState,
                    modifier = Modifier.align(Alignment.TopCenter),
                    backgroundColor = colors.surface,
                    contentColor = colors.accent,
                )
            }
        }

        if (showSearch) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.72f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { showSearch = false },
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .offset(y = 56.dp)
                        .background(colors.surface, RoundedCornerShape(16.dp))
                        .border(1.dp, colors.border, RoundedCornerShape(16.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {}
                        .padding(14.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.surface2, RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AppIcon(R.drawable.ic_search, null, tint = colors.muted, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(10.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            if (query.isBlank()) {
                                Text("Search for a city…", color = colors.muted, fontFamily = font, fontSize = 14.sp)
                            }
                            BasicTextField(
                                value = query,
                                onValueChange = { query = it; onSearchQueryChange(it) },
                                textStyle = TextStyle(color = colors.text, fontFamily = font, fontSize = 14.sp),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) {
                                onUseCurrentLocation()
                                showSearch = false
                            }
                            .padding(horizontal = 4.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AppIcon(R.drawable.ic_gps, null, tint = colors.accent, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("Use current location", color = colors.accent, fontFamily = font, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Divider()
                    Column(modifier = Modifier.padding(top = 6.dp)) {
                        searchResults.forEach { result ->
                            TextButton(onClick = {
                                showSearch = false
                                query = ""
                                onSelectSearchResult(result)
                            }) {
                                Text(
                                    text = listOfNotNull(result.name, result.country).joinToString(", "),
                                    color = colors.text,
                                    fontFamily = font,
                                    fontSize = 14.sp,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun locationTitle(state: WeatherUiState): String = when (state) {
    is WeatherUiState.Success -> state.snapshot.locationName
    is WeatherUiState.Stale -> state.snapshot.locationName
    else -> "Ventus"
}

private fun staleLabel(fetchedAt: Long): String {
    val minutesAgo = (System.currentTimeMillis() - fetchedAt) / 60000
    return when {
        minutesAgo < 1 -> "Updated just now"
        minutesAgo < 60 -> "Updated ${minutesAgo}m ago"
        else -> "Updated ${minutesAgo / 60}h ago"
    }
}

@Composable
private fun LoadingBody() {
    val colors = LocalAppColors.current
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Spinner(color = colors.accent, modifier = Modifier.size(32.dp))
    }
}

@Composable
private fun NeedsLocationBody(onUseCurrentLocation: () -> Unit) {
    val colors = LocalAppColors.current
    val font = LocalAppFontFamily.current
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Find your weather", color = colors.text, fontFamily = font, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Use your current location or search for a city above.",
            color = colors.muted,
            fontFamily = font,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onUseCurrentLocation) {
            Text("Use my location", color = colors.accent, fontFamily = font, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ErrorBody(message: String, onRetry: () -> Unit) {
    val colors = LocalAppColors.current
    val font = LocalAppFontFamily.current
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(message, color = colors.error, fontFamily = font, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onRetry) {
            Text("Retry", color = colors.accent, fontFamily = font, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ForecastBody(snapshot: WeatherSnapshot, staleBanner: String?) {
    val colors = LocalAppColors.current
    val font = LocalAppFontFamily.current
    val isImperial = snapshot.units == Units.IMPERIAL
    val today = snapshot.daily.getOrNull(0)

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        if (staleBanner != null) {
            Box(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp), contentAlignment = Alignment.Center) {
                Row(
                    modifier = Modifier
                        .background(colors.surface, RoundedCornerShape(100.dp))
                        .border(1.dp, colors.border, RoundedCornerShape(100.dp))
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                ) {
                    Text(staleBanner, color = colors.muted, fontFamily = font, fontSize = 11.sp)
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(start = 24.dp, top = 12.dp, end = 24.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val info = weatherCodeInfo(snapshot.currentWeatherCode)
            AppIcon(info.icon, info.description, tint = colors.accent, modifier = Modifier.size(64.dp))
            Text(
                text = "${tempValue(snapshot.currentTempC, isImperial)}°",
                color = colors.text,
                fontFamily = font,
                fontSize = 64.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-2).sp,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                info.description,
                color = colors.text,
                fontFamily = font,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 6.dp),
            )
            if (today != null) {
                Text(
                    text = "H:${tempValue(today.tempMaxC, isImperial)}° L:${tempValue(today.tempMinC, isImperial)}° · Feels like ${tempValue(snapshot.currentApparentTempC, isImperial)}°",
                    color = colors.muted,
                    fontFamily = font,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val wind = if (isImperial) kmhToMph(snapshot.currentWindKmh) else snapshot.currentWindKmh
            StatChip(R.drawable.ic_thermometer, "${tempValue(snapshot.currentApparentTempC, isImperial)}°", "Feels like")
            StatChip(R.drawable.ic_droplet, "${snapshot.currentHumidity}%", "Humidity")
            StatChip(R.drawable.ic_wind, "${wind.roundToInt()}${if (isImperial) "mph" else "km/h"}", "Wind")
            StatChip(R.drawable.ic_weather_sun, snapshot.uvIndex?.roundToInt()?.toString() ?: "—", "UV Index")
            StatChip(R.drawable.ic_droplet, "${today?.precipitationProbability ?: 0}%", "Precipitation")
        }

        SectionCard(
            label = "24-hour forecast",
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 16.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        ) {
            LazyRow(modifier = Modifier.fillMaxWidth()) {
                items(snapshot.hourly) { hour ->
                    val hourInfo = weatherCodeInfo(hour.weatherCode)
                    Column(
                        modifier = Modifier.width(56.dp).padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(hourLabel(hour.epochSeconds), color = colors.muted, fontFamily = font, fontSize = 11.5.sp)
                        AppIcon(hourInfo.icon, hourInfo.description, tint = colors.text, modifier = Modifier.size(22.dp))
                        Text("${hour.precipitationProbability}%", color = colors.accent, fontFamily = font, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        Text("${tempValue(hour.tempC, isImperial)}°", color = colors.text, fontFamily = font, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        val sunrise = snapshot.sunriseEpochSeconds
        val sunset = snapshot.sunsetEpochSeconds
        if (sunrise != null && sunset != null) {
            SectionCard(
                label = "Sun",
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 16.dp),
            ) {
                SunArc(
                    sunriseEpochSeconds = sunrise,
                    sunsetEpochSeconds = sunset,
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                )
                Spacer(Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(timeLabel(sunrise), color = colors.text, fontFamily = font, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("SUNRISE", color = colors.muted, fontFamily = font, fontSize = 10.5.sp, letterSpacing = 0.4.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(timeLabel(sunset), color = colors.text, fontFamily = font, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("SUNSET", color = colors.muted, fontFamily = font, fontSize = 10.5.sp, letterSpacing = 0.4.sp)
                    }
                }
            }
        }

        val aqi = snapshot.aqi
        if (aqi != null) {
            val info = aqiInfo(aqi)
            SectionCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(
                        modifier = Modifier.size(52.dp).background(colors.accent.copy(alpha = 0.12f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("$aqi", color = colors.accent, fontFamily = font, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Column {
                        Text("Air quality — ${info.category}", color = colors.text, fontFamily = font, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(info.description, color = colors.muted, fontFamily = font, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                    }
                }
            }
        }

        val weekMin = snapshot.daily.minOfOrNull { it.tempMinC } ?: 0.0
        val weekMax = snapshot.daily.maxOfOrNull { it.tempMaxC } ?: 0.0
        val span = (weekMax - weekMin).coerceAtLeast(0.001)

        SectionCard(
            label = "7-day forecast",
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 6.dp),
        ) {
            snapshot.daily.forEachIndexed { index, day ->
                if (index > 0) Divider()
                val dayInfo = weatherCodeInfo(day.weatherCode)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(dayLabel(day.epochSeconds), color = colors.text, fontFamily = font, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(38.dp))
                    AppIcon(dayInfo.icon, dayInfo.description, tint = colors.text, modifier = Modifier.size(22.dp))
                    Text("${day.precipitationProbability}%", color = colors.accent, fontFamily = font, fontSize = 11.sp, modifier = Modifier.width(30.dp))
                    Text(
                        "${tempValue(day.tempMinC, isImperial)}°",
                        color = colors.muted,
                        fontFamily = font,
                        fontSize = 13.sp,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(26.dp),
                    )
                    RangeBar(
                        leftFraction = ((day.tempMinC - weekMin) / span).toFloat(),
                        widthFraction = ((day.tempMaxC - day.tempMinC) / span).toFloat(),
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "${tempValue(day.tempMaxC, isImperial)}°",
                        color = colors.text,
                        fontFamily = font,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(26.dp),
                    )
                }
            }
        }
    }
}

private fun tempValue(c: Double, isImperial: Boolean): Int {
    val v = if (isImperial) celsiusToFahrenheit(c) else c
    return v.roundToInt()
}

private fun hourLabel(epochSeconds: Long): String =
    SimpleDateFormat("ha", Locale.getDefault()).format(Date(epochSeconds * 1000))

private fun dayLabel(epochSeconds: Long): String =
    SimpleDateFormat("EEE", Locale.getDefault()).format(Date(epochSeconds * 1000))

private fun timeLabel(epochSeconds: Long): String =
    SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(epochSeconds * 1000))
