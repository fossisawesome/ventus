package com.fossisawesome.ventus.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fossisawesome.ventus.data.isUsLocation
import com.fossisawesome.ventus.data.repository.LocationRepository
import com.fossisawesome.ventus.data.storage.AppPreferences
import com.fossisawesome.ventus.ui.theme.AppTheme
import com.fossisawesome.ventus.work.BackgroundRefreshScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// importTheme/deleteTheme take a plain String (the picked file's URI, stringified) rather than
// android.net.Uri directly — Uri is unusable in plain JUnit tests without Robolectric (it's a
// platform type whose real methods throw outside an Android runtime), so keeping this class
// framework-agnostic makes it fully unit-testable. MainActivity converts to/from Uri at the edge.
class SettingsViewModel(
    private val prefs: AppPreferences,
    private val locationRepository: LocationRepository,
    private val scheduler: BackgroundRefreshScheduler,
    private val loadThemes: () -> List<AppTheme>,
    private val importTheme: (String) -> Result<Unit>,
    private val deleteTheme: (String) -> Unit,
) : ViewModel() {

    val themeId: StateFlow<String> = prefs.themeId.stateIn(viewModelScope, SharingStarted.Eagerly, "ventus")
    val fontFamily: StateFlow<String> = prefs.fontFamily.stateIn(viewModelScope, SharingStarted.Eagerly, "Liberation Mono")
    val unitsMode: StateFlow<String> = prefs.unitsMode.stateIn(viewModelScope, SharingStarted.Eagerly, "auto")
    val weatherProvider: StateFlow<String> = prefs.weatherProvider.stateIn(viewModelScope, SharingStarted.Eagerly, "open-meteo")
    val backgroundRefreshEnabled: StateFlow<Boolean> = prefs.backgroundRefreshEnabled.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val backgroundRefreshIntervalMinutes: StateFlow<Int> = prefs.backgroundRefreshIntervalMinutes.stateIn(viewModelScope, SharingStarted.Eagerly, 60)

    // Drives whether the NWS option is selectable in Settings — NWS has no coverage outside the
    // US, gated on the CURRENTLY ACTIVE saved location's coordinates (see isUsLocation()); this
    // deliberately does not consider any of the user's OTHER saved locations.
    val isNwsAvailable: StateFlow<Boolean> = combine(
        locationRepository.locationsFlow,
        locationRepository.activeLocationIdFlow,
    ) { locations, activeId ->
        val active = locations.find { it.id == activeId }
        active != null && isUsLocation(active.lat, active.lon)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _availableThemes = MutableStateFlow(loadThemes())
    val availableThemes: StateFlow<List<AppTheme>> = _availableThemes

    fun selectTheme(id: String) {
        viewModelScope.launch { prefs.setThemeId(id) }
    }

    fun selectFont(name: String) {
        viewModelScope.launch { prefs.setFontFamily(name) }
    }

    fun selectUnitsMode(mode: String) {
        viewModelScope.launch { prefs.setUnitsMode(mode) }
    }

    fun selectWeatherProvider(id: String) {
        viewModelScope.launch { prefs.setWeatherProvider(id) }
    }

    fun setBackgroundRefreshEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefs.setBackgroundRefreshEnabled(enabled)
            if (enabled) scheduler.schedule(prefs.backgroundRefreshIntervalMinutes.first()) else scheduler.cancel()
        }
    }

    fun setBackgroundRefreshIntervalMinutes(minutes: Int) {
        viewModelScope.launch {
            prefs.setBackgroundRefreshIntervalMinutes(minutes)
            if (prefs.backgroundRefreshEnabled.first()) scheduler.schedule(minutes)
        }
    }

    fun importThemeFile(uriString: String): Result<Unit> {
        val result = importTheme(uriString)
        if (result.isSuccess) _availableThemes.value = loadThemes()
        return result
    }

    fun deleteThemeFile(theme: AppTheme) {
        val file = theme.sourceFile ?: return
        deleteTheme(file)
        _availableThemes.value = loadThemes()
    }
}
