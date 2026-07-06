package com.fossisawesome.ventus.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.fossisawesome.ventus.ui.screens.MainScreen
import com.fossisawesome.ventus.ui.screens.MapScreen
import com.fossisawesome.ventus.ui.screens.SettingsScreen
import com.fossisawesome.ventus.viewmodel.RadarViewModel
import com.fossisawesome.ventus.viewmodel.SettingsViewModel
import com.fossisawesome.ventus.viewmodel.WeatherViewModel

@Composable
fun AppNavGraph(
    weatherViewModel: WeatherViewModel,
    settingsViewModel: SettingsViewModel,
    radarViewModel: RadarViewModel,
    onImportTheme: () -> Unit,
) {
    val navController = rememberNavController()
    val locations by weatherViewModel.locations.collectAsStateWithLifecycle()
    val activeLocationId by weatherViewModel.activeLocationId.collectAsStateWithLifecycle()
    val weatherStates by weatherViewModel.weatherStates.collectAsStateWithLifecycle()
    val searchResults by weatherViewModel.searchResults.collectAsStateWithLifecycle()
    val locationLimitMessage by weatherViewModel.locationLimitMessage.collectAsStateWithLifecycle()
    val themeId by settingsViewModel.themeId.collectAsStateWithLifecycle()
    val fontFamily by settingsViewModel.fontFamily.collectAsStateWithLifecycle()
    val unitsMode by settingsViewModel.unitsMode.collectAsStateWithLifecycle()
    val weatherProvider by settingsViewModel.weatherProvider.collectAsStateWithLifecycle()
    val isNwsAvailable by settingsViewModel.isNwsAvailable.collectAsStateWithLifecycle()
    val backgroundRefreshEnabled by settingsViewModel.backgroundRefreshEnabled.collectAsStateWithLifecycle()
    val backgroundRefreshIntervalMinutes by settingsViewModel.backgroundRefreshIntervalMinutes.collectAsStateWithLifecycle()
    val availableThemes by settingsViewModel.availableThemes.collectAsStateWithLifecycle()
    val radarFrames by radarViewModel.frames.collectAsStateWithLifecycle()
    val radarCurrentFrameIndex by radarViewModel.currentFrameIndex.collectAsStateWithLifecycle()
    val radarIsPlaying by radarViewModel.isPlaying.collectAsStateWithLifecycle()
    val radarActiveLocation by radarViewModel.activeLocation.collectAsStateWithLifecycle()

    NavHost(navController = navController, startDestination = "main") {
        composable("main") {
            MainScreen(
                locations = locations,
                activeLocationId = activeLocationId,
                weatherStates = weatherStates,
                searchResults = searchResults,
                locationLimitMessage = locationLimitMessage,
                onPageSelected = { weatherViewModel.onPageSelected(it) },
                onRefresh = { weatherViewModel.onPageSelected(it) }, // pull-to-refresh forces a refresh on the same page it's already showing
                onUseCurrentLocation = { weatherViewModel.useCurrentLocation() },
                onSearchQueryChange = { weatherViewModel.search(it) },
                onAddLocation = { weatherViewModel.addLocationFromSearch(it) },
                onRemoveLocation = { weatherViewModel.removeLocation(it) },
                onReorderLocations = { weatherViewModel.reorderLocations(it) },
                onSelectLocation = { weatherViewModel.selectLocation(it) },
                onDismissLocationLimitMessage = { weatherViewModel.dismissLocationLimitMessage() },
                onRadarClick = { navController.navigate("radar") },
                onSettingsClick = { navController.navigate("settings") },
            )
        }
        composable("radar") {
            LaunchedEffect(Unit) { radarViewModel.loadFrames() }
            MapScreen(
                activeLocation = radarActiveLocation,
                frames = radarFrames,
                currentFrameIndex = radarCurrentFrameIndex,
                isPlaying = radarIsPlaying,
                onTogglePlayback = { radarViewModel.togglePlayback() },
                onAdvanceFrame = { radarViewModel.advanceFrame() },
                onBack = { navController.popBackStack() },
            )
        }
        composable("settings") {
            SettingsScreen(
                themeId = themeId,
                fontFamily = fontFamily,
                unitsMode = unitsMode,
                weatherProvider = weatherProvider,
                isNwsAvailable = isNwsAvailable,
                backgroundRefreshEnabled = backgroundRefreshEnabled,
                backgroundRefreshIntervalMinutes = backgroundRefreshIntervalMinutes,
                availableThemes = availableThemes,
                onThemeSelected = { settingsViewModel.selectTheme(it) },
                onFontSelected = { settingsViewModel.selectFont(it) },
                onUnitsModeSelected = { settingsViewModel.selectUnitsMode(it) },
                onWeatherProviderSelected = { settingsViewModel.selectWeatherProvider(it) },
                onBackgroundRefreshEnabledChanged = { settingsViewModel.setBackgroundRefreshEnabled(it) },
                onBackgroundRefreshIntervalSelected = { settingsViewModel.setBackgroundRefreshIntervalMinutes(it) },
                onImportTheme = onImportTheme,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
