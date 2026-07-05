package com.fossisawesome.ventus.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.fossisawesome.ventus.ui.screens.MainScreen
import com.fossisawesome.ventus.ui.screens.SettingsScreen
import com.fossisawesome.ventus.viewmodel.SettingsViewModel
import com.fossisawesome.ventus.viewmodel.WeatherViewModel

@Composable
fun AppNavGraph(
    weatherViewModel: WeatherViewModel,
    settingsViewModel: SettingsViewModel,
    onImportTheme: () -> Unit,
) {
    val navController = rememberNavController()
    val weatherState by weatherViewModel.state.collectAsStateWithLifecycle()
    val searchResults by weatherViewModel.searchResults.collectAsStateWithLifecycle()
    val themeId by settingsViewModel.themeId.collectAsStateWithLifecycle()
    val fontFamily by settingsViewModel.fontFamily.collectAsStateWithLifecycle()
    val unitsMode by settingsViewModel.unitsMode.collectAsStateWithLifecycle()
    val weatherProvider by settingsViewModel.weatherProvider.collectAsStateWithLifecycle()
    val isNwsAvailable by settingsViewModel.isNwsAvailable.collectAsStateWithLifecycle()
    val availableThemes by settingsViewModel.availableThemes.collectAsStateWithLifecycle()

    NavHost(navController = navController, startDestination = "main") {
        composable("main") {
            MainScreen(
                state = weatherState,
                searchResults = searchResults,
                onRefresh = { weatherViewModel.refresh() },
                onUseCurrentLocation = { weatherViewModel.useCurrentLocation() },
                onSearchQueryChange = { weatherViewModel.search(it) },
                onSelectSearchResult = { weatherViewModel.selectLocation(it) },
                onSettingsClick = { navController.navigate("settings") },
            )
        }
        composable("settings") {
            SettingsScreen(
                themeId = themeId,
                fontFamily = fontFamily,
                unitsMode = unitsMode,
                weatherProvider = weatherProvider,
                isNwsAvailable = isNwsAvailable,
                availableThemes = availableThemes,
                onThemeSelected = { settingsViewModel.selectTheme(it) },
                onFontSelected = { settingsViewModel.selectFont(it) },
                onUnitsModeSelected = { settingsViewModel.selectUnitsMode(it) },
                onWeatherProviderSelected = { settingsViewModel.selectWeatherProvider(it) },
                onImportTheme = onImportTheme,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
