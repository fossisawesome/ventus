package com.fossisawesome.ventus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fossisawesome.ventus.data.*
import com.fossisawesome.ventus.data.model.*
import com.fossisawesome.ventus.ui.components.*
import com.fossisawesome.ventus.ui.theme.LocalAppColors
import com.fossisawesome.ventus.ui.theme.LocalAppFontFamily
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = locationTitle(state),
                    color = colors.text,
                    fontFamily = font,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { showSearch = !showSearch }, modifier = Modifier.size(40.dp)) {
                    AppIcon(Icons.Default.Search, "Search for a city", tint = colors.muted)
                }
                IconButton(onClick = onUseCurrentLocation, modifier = Modifier.size(40.dp)) {
                    AppIcon(Icons.Default.MyLocation, "Use current location", tint = colors.muted)
                }
                IconButton(onClick = onSettingsClick, modifier = Modifier.size(40.dp)) {
                    AppIcon(Icons.Default.Settings, "Settings", tint = colors.muted)
                }
            }

            if (showSearch) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it; onSearchQueryChange(it) },
                        textStyle = TextStyle(color = colors.text, fontFamily = font, fontSize = 16.sp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.surface)
                            .padding(12.dp),
                    )
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
                            )
                        }
                    }
                }
            }

            Divider()

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
        minutesAgo < 1 -> "Updated just now — pull to refresh"
        minutesAgo < 60 -> "Updated ${minutesAgo}m ago — pull to refresh"
        else -> "Updated ${minutesAgo / 60}h ago — pull to refresh"
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
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
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
        Text(message, color = colors.error, fontFamily = font, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
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

    fun displayTemp(c: Double): String {
        val v = if (isImperial) celsiusToFahrenheit(c) else c
        return "${v.toInt()}°${if (isImperial) "F" else "C"}"
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        if (staleBanner != null) {
            Box(modifier = Modifier.fillMaxWidth().background(colors.surface2).padding(8.dp)) {
                Text(staleBanner, color = colors.muted, fontFamily = font, fontSize = 12.sp)
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val info = weatherCodeInfo(snapshot.currentWeatherCode)
            Text(info.icon, fontSize = 48.sp)
            Text(displayTemp(snapshot.currentTempC), color = colors.text, fontFamily = font, fontSize = 48.sp, fontWeight = FontWeight.Bold)
            Text(info.description, color = colors.muted, fontFamily = font)
            Spacer(Modifier.height(8.dp))
            Row {
                Text("Feels like ${displayTemp(snapshot.currentApparentTempC)}", color = colors.muted, fontFamily = font, fontSize = 13.sp)
                Spacer(Modifier.width(16.dp))
                Text("Humidity ${snapshot.currentHumidity}%", color = colors.muted, fontFamily = font, fontSize = 13.sp)
                Spacer(Modifier.width(16.dp))
                val wind = if (isImperial) kmhToMph(snapshot.currentWindKmh) else snapshot.currentWindKmh
                Text("Wind ${wind.toInt()} ${if (isImperial) "mph" else "km/h"}", color = colors.muted, fontFamily = font, fontSize = 13.sp)
            }
        }

        Divider()

        LazyRow(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
            items(snapshot.hourly) { hour ->
                Column(
                    modifier = Modifier.width(64.dp).padding(horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(hourLabel(hour.epochSeconds), color = colors.muted, fontFamily = font, fontSize = 12.sp)
                    Text(weatherCodeInfo(hour.weatherCode).icon, fontSize = 20.sp)
                    Text(displayTemp(hour.tempC), color = colors.text, fontFamily = font, fontSize = 13.sp)
                }
            }
        }

        Divider()

        Column(modifier = Modifier.fillMaxWidth()) {
            snapshot.daily.forEach { day ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(dayLabel(day.epochSeconds), color = colors.text, fontFamily = font, modifier = Modifier.weight(1f))
                    Text(weatherCodeInfo(day.weatherCode).icon, fontSize = 18.sp)
                    Spacer(Modifier.width(12.dp))
                    Text(displayTemp(day.tempMinC), color = colors.muted, fontFamily = font)
                    Spacer(Modifier.width(8.dp))
                    Text(displayTemp(day.tempMaxC), color = colors.text, fontFamily = font, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

private fun hourLabel(epochSeconds: Long): String =
    SimpleDateFormat("ha", Locale.getDefault()).format(Date(epochSeconds * 1000))

private fun dayLabel(epochSeconds: Long): String =
    SimpleDateFormat("EEE", Locale.getDefault()).format(Date(epochSeconds * 1000))
