package com.fossisawesome.ventus.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Map
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
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
import java.util.TimeZone
import kotlin.math.roundToInt
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(androidx.compose.material.ExperimentalMaterialApi::class, ExperimentalFoundationApi::class)
@Composable
fun MainScreen(
    locations: List<Location>,
    activeLocationId: String?,
    weatherStates: Map<String, WeatherUiState>,
    searchResults: List<GeocodingResult>,
    locationLimitMessage: String?,
    onPageSelected: (String) -> Unit,
    onRefresh: (String) -> Unit,
    onUseCurrentLocation: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onAddLocation: (GeocodingResult) -> Unit,
    onRemoveLocation: (String) -> Unit,
    onReorderLocations: (List<String>) -> Unit,
    onSelectLocation: (String) -> Unit,
    onDismissLocationLimitMessage: () -> Unit,
    onRadarClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    val colors = LocalAppColors.current
    val font = LocalAppFontFamily.current
    var showPicker by remember { mutableStateOf(false) }

    val pagerState = rememberPagerState(pageCount = { locations.size })

    // Keeps the pager in sync with the persisted active location — e.g. a picker-row tap jumps to
    // a non-adjacent page, or the first location arrives asynchronously after loadInitial().
    LaunchedEffect(activeLocationId, locations) {
        val targetIndex = locations.indexOfFirst { it.id == activeLocationId }
        if (targetIndex >= 0 && targetIndex != pagerState.currentPage) {
            pagerState.scrollToPage(targetIndex)
        }
    }

    // Fires once the user's swipe settles on a new page (settledPage, not currentPage, so this
    // doesn't fire on every drag frame mid-swipe).
    LaunchedEffect(pagerState, locations) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collect { index -> locations.getOrNull(index)?.let { onPageSelected(it.id) } }
    }

    Box(modifier = Modifier.fillMaxSize().background(colors.bg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(20.dp, 20.dp, 20.dp, 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { showPicker = !showPicker }, modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        AppIcon(R.drawable.ic_pin, null, tint = colors.accent, modifier = Modifier.size(20.dp))
                        Text(
                            text = locations.getOrNull(pagerState.currentPage)?.name ?: "Ventus",
                            color = colors.text,
                            fontFamily = font,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.2).sp,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        AppIcon(R.drawable.ic_chevron_right, null, tint = colors.muted, modifier = Modifier.size(16.dp))
                    }
                }
                IconButton(
                    onClick = onRadarClick,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.accent.copy(alpha = 0.08f)),
                ) {
                    AppIcon(Icons.Default.Map, "Radar", tint = colors.accent, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(8.dp))
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

            if (locations.isEmpty()) {
                NeedsLocationBody(onUseCurrentLocation)
            } else {
                HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
                    val location = locations[page]
                    val state = weatherStates[location.id] ?: WeatherUiState.Loading
                    val isRefreshing = state is WeatherUiState.Loading
                    val pullState = rememberPullRefreshState(refreshing = isRefreshing, onRefresh = { onRefresh(location.id) })

                    Box(modifier = Modifier.fillMaxSize().pullRefresh(pullState)) {
                        when (state) {
                            is WeatherUiState.Loading -> LoadingBody()
                            is WeatherUiState.NeedsLocation -> LoadingBody() // transient — a refresh is already in flight for this page
                            is WeatherUiState.Error -> ErrorBody(state.message, onRetry = { onRefresh(location.id) })
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
                PageDots(
                    pageCount = locations.size,
                    currentPage = pagerState.currentPage,
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 12.dp),
                )
            }
        }

        if (showPicker) {
            LocationPickerOverlay(
                locations = locations,
                searchResults = searchResults,
                locationLimitMessage = locationLimitMessage,
                onDismiss = { showPicker = false },
                onSearchQueryChange = onSearchQueryChange,
                onUseCurrentLocation = { onUseCurrentLocation(); showPicker = false },
                onAddLocation = { result -> onAddLocation(result); showPicker = false },
                onSelectLocation = { id -> onSelectLocation(id); showPicker = false },
                onRemoveLocation = onRemoveLocation,
                onReorderLocations = onReorderLocations,
                onDismissLocationLimitMessage = onDismissLocationLimitMessage,
            )
        }
    }
}

// Generalizes the old search-only overlay into a full location picker: search-to-add at the top,
// then the user's saved locations below with drag-to-reorder (long-press the row, drag by the
// handle) and a delete button per row. Tapping a saved row (not the handle/delete icon) selects it.
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LocationPickerOverlay(
    locations: List<Location>,
    searchResults: List<GeocodingResult>,
    locationLimitMessage: String?,
    onDismiss: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onUseCurrentLocation: () -> Unit,
    onAddLocation: (GeocodingResult) -> Unit,
    onSelectLocation: (String) -> Unit,
    onRemoveLocation: (String) -> Unit,
    onReorderLocations: (List<String>) -> Unit,
    onDismissLocationLimitMessage: () -> Unit,
) {
    val colors = LocalAppColors.current
    val font = LocalAppFontFamily.current
    var query by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .offset(y = 56.dp)
                .background(colors.surface, RoundedCornerShape(16.dp))
                .border(1.dp, colors.border, RoundedCornerShape(16.dp))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
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

            if (query.isBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onUseCurrentLocation)
                        .padding(horizontal = 4.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppIcon(R.drawable.ic_gps, null, tint = colors.accent, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Use current location", color = colors.accent, fontFamily = font, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
                Divider()
            }

            if (query.isNotBlank()) {
                Column(modifier = Modifier.padding(top = 6.dp)) {
                    searchResults.forEach { result ->
                        TextButton(onClick = { query = ""; onAddLocation(result) }) {
                            Text(
                                text = listOfNotNull(result.name, result.country).joinToString(", "),
                                color = colors.text,
                                fontFamily = font,
                                fontSize = 14.sp,
                            )
                        }
                    }
                }
            } else {
                if (locationLimitMessage != null) {
                    Text(
                        locationLimitMessage,
                        color = colors.error,
                        fontFamily = font,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(vertical = 8.dp).clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onDismissLocationLimitMessage() },
                    )
                }
                Text(
                    "SAVED LOCATIONS".uppercase(),
                    color = colors.muted,
                    fontFamily = font,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp,
                    modifier = Modifier.padding(top = 6.dp, bottom = 4.dp),
                )
                ReorderableLocationList(
                    locations = locations,
                    onSelect = onSelectLocation,
                    onRemove = onRemoveLocation,
                    onReorder = onReorderLocations,
                )
            }
        }
    }
}

// Manual long-press-drag reorder — no third-party reorder library exists in this project's
// dependencies, so item order is swapped in-place as the dragged row's midpoint crosses a
// neighbor's, using each row's measured height (via onGloballyPositioned) as the swap threshold.
@Composable
private fun ReorderableLocationList(
    locations: List<Location>,
    onSelect: (String) -> Unit,
    onRemove: (String) -> Unit,
    onReorder: (List<String>) -> Unit,
) {
    val colors = LocalAppColors.current
    val font = LocalAppFontFamily.current

    var orderedIds by remember(locations) { mutableStateOf(locations.map { it.id }) }
    val byId = locations.associateBy { it.id }
    var draggingId by remember { mutableStateOf<String?>(null) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    val rowHeights = remember { mutableStateMapOf<String, Float>() }

    Column(modifier = Modifier.fillMaxWidth()) {
        orderedIds.forEachIndexed { index, id ->
            val location = byId[id] ?: return@forEachIndexed
            val isDragging = draggingId == id
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { rowHeights[id] = it.size.height.toFloat() }
                    .offset { IntOffset(0, if (isDragging) dragOffsetY.toInt() else 0) }
                    .background(if (isDragging) colors.surface2 else Color.Transparent, RoundedCornerShape(8.dp))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onSelect(id) }
                    .padding(horizontal = 4.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    location.name,
                    color = colors.text,
                    fontFamily = font,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { onRemove(id) }, modifier = Modifier.size(28.dp)) {
                    AppIcon(Icons.Filled.Close, "Remove", tint = colors.muted, modifier = Modifier.size(16.dp))
                }
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .pointerInput(id) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { draggingId = id; dragOffsetY = 0f },
                                onDragEnd = {
                                    draggingId = null
                                    dragOffsetY = 0f
                                    onReorder(orderedIds)
                                },
                                onDragCancel = { draggingId = null; dragOffsetY = 0f },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragOffsetY += dragAmount.y
                                    val rowHeight = rowHeights[id] ?: return@detectDragGesturesAfterLongPress
                                    val currentIndex = orderedIds.indexOf(id)
                                    if (dragOffsetY > rowHeight / 2 && currentIndex < orderedIds.lastIndex) {
                                        orderedIds = orderedIds.toMutableList().apply { add(currentIndex + 1, removeAt(currentIndex)) }
                                        dragOffsetY -= rowHeight
                                    } else if (dragOffsetY < -rowHeight / 2 && currentIndex > 0) {
                                        orderedIds = orderedIds.toMutableList().apply { add(currentIndex - 1, removeAt(currentIndex)) }
                                        dragOffsetY += rowHeight
                                    }
                                },
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    AppIcon(Icons.Filled.DragHandle, "Reorder", tint = colors.muted, modifier = Modifier.size(18.dp))
                }
            }
            if (index < orderedIds.lastIndex) Divider()
        }
    }
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
                val feelsLikeSuffix = snapshot.currentApparentTempC?.let { " · Feels like ${tempValue(it, isImperial)}°" } ?: ""
                Text(
                    text = "H:${tempValue(today.tempMaxC, isImperial)}° L:${tempValue(today.tempMinC, isImperial)}°$feelsLikeSuffix",
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
            snapshot.currentApparentTempC?.let {
                StatChip(R.drawable.ic_thermometer, "${tempValue(it, isImperial)}°", "Feels like")
            }
            snapshot.currentHumidity?.let {
                StatChip(R.drawable.ic_droplet, "$it%", "Humidity")
            }
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

// Epoch values are built by isoLocalTimeToEpochSeconds(), which mislabels the API's
// location-local wall-clock time as UTC. Formatting must use UTC here to cancel that
// mislabeling out and recover the correct location-local time, regardless of device timezone.
private fun locationLocalFormat(pattern: String, epochSeconds: Long): String =
    SimpleDateFormat(pattern, Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }.format(Date(epochSeconds * 1000))

private fun hourLabel(epochSeconds: Long): String = locationLocalFormat("ha", epochSeconds)

private fun dayLabel(epochSeconds: Long): String = locationLocalFormat("EEE", epochSeconds)

private fun timeLabel(epochSeconds: Long): String = locationLocalFormat("h:mm a", epochSeconds)
