package com.fossisawesome.ventus.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fossisawesome.ventus.data.api.GeocodingApi
import com.fossisawesome.ventus.data.location.LocationResult
import com.fossisawesome.ventus.data.location.LocationSource
import com.fossisawesome.ventus.data.location.resolveLocationCoords
import com.fossisawesome.ventus.data.model.GeocodingResult
import com.fossisawesome.ventus.data.model.Location
import com.fossisawesome.ventus.data.model.WeatherUiState
import com.fossisawesome.ventus.data.model.resolveActiveLocationId
import com.fossisawesome.ventus.data.model.toSavedLocation
import com.fossisawesome.ventus.data.repository.LocationRepository
import com.fossisawesome.ventus.data.repository.WeatherRepository
import com.fossisawesome.ventus.data.resolveUnits
import com.fossisawesome.ventus.data.storage.AppPreferences
import com.fossisawesome.ventus.work.WidgetUpdater
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WeatherViewModel(
    private val weatherRepository: WeatherRepository,
    private val locationRepository: LocationRepository,
    private val locationSource: LocationSource,
    private val geocodingApi: GeocodingApi,
    private val prefs: AppPreferences,
    private val countryCode: String,
    private val widgetUpdater: WidgetUpdater,
) : ViewModel() {

    // Both flow directly off DataStore (via LocationRepository) — mutations below only ever write
    // through the repository, never mutate a local snapshot, so this ViewModel and
    // SettingsViewModel's isNwsAvailable (which reads the same flows independently) can never
    // drift out of sync with each other or with what's actually persisted.
    val locations: StateFlow<List<Location>> = locationRepository.locationsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val activeLocationId: StateFlow<String?> = locationRepository.activeLocationIdFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _weatherStates = MutableStateFlow<Map<String, WeatherUiState>>(emptyMap())
    val weatherStates: StateFlow<Map<String, WeatherUiState>> = _weatherStates.asStateFlow()

    private val _searchResults = MutableStateFlow<List<GeocodingResult>>(emptyList())
    val searchResults: StateFlow<List<GeocodingResult>> = _searchResults.asStateFlow()

    private val _locationLimitMessage = MutableStateFlow<String?>(null)
    val locationLimitMessage: StateFlow<String?> = _locationLimitMessage.asStateFlow()

    // Guards against stacking duplicate concurrent refreshes for the same location if the user
    // swipes back and forth across the same page quickly.
    private val refreshingIds = mutableSetOf<String>()

    // On first load: migrate any pre-multi-location data, then seed every saved page with
    // whatever's cached (so swiping never shows a blank spinner for a page visited before), and do
    // a single live refresh for just the active page — NOT all pages, to avoid hammering the
    // weather API for up to 10 locations on every app open.
    fun loadInitial() {
        viewModelScope.launch {
            val migrated = locationRepository.migrateIfNeeded()
            if (migrated != null) weatherRepository.migrateLegacyCache(migrated.id)

            val savedLocations = locationRepository.locationsFlow.first()
            if (savedLocations.isEmpty()) {
                attemptAddCurrentLocation()
                return@launch
            }

            savedLocations.forEach { loc ->
                _weatherStates.update { it + (loc.id to weatherRepository.loadCached(loc.id)) }
            }
            val activeId = resolveActiveLocationId(savedLocations, locationRepository.activeLocationIdFlow.first())
            if (activeId != null) {
                locationRepository.setActiveLocationId(activeId)
                refreshLocation(activeId)
            }
        }
    }

    // Called by MainScreen once the pager settles on a new page (not on every drag frame).
    fun onPageSelected(locationId: String) {
        viewModelScope.launch { locationRepository.setActiveLocationId(locationId) }
        refreshLocation(locationId)
    }

    // Pull-to-refresh on whichever page is currently active.
    fun refreshActive() {
        activeLocationId.value?.let { refreshLocation(it) }
    }

    private fun refreshLocation(locationId: String) {
        if (!refreshingIds.add(locationId)) return
        viewModelScope.launch {
            try {
                val location = locations.value.find { it.id == locationId } ?: return@launch
                val (lat, lon) = resolveLocationCoords(location, locationSource)
                _weatherStates.update { it + (locationId to WeatherUiState.Loading) }
                val unitsMode = prefs.unitsMode.first()
                val units = resolveUnits(unitsMode, countryCode)
                val result = weatherRepository.refresh(locationId, lat, lon, location.name, units)
                _weatherStates.update { it + (locationId to result) }
                if (location.isCurrentLocation) locationRepository.upsertCurrentLocationCoords(lat, lon)
                if (locationId == activeLocationId.value) widgetUpdater.notifyActiveLocationChanged()
            } finally {
                refreshingIds.remove(locationId)
            }
        }
    }

    // Tapped from the picker's "Use current location" row. If a GPS entry already exists, just
    // jumps to it (which also triggers a fresh coordinate re-resolution via refreshLocation);
    // otherwise attempts to add one for the first time.
    fun useCurrentLocation() {
        viewModelScope.launch {
            val existing = locations.value.find { it.isCurrentLocation }
            if (existing != null) {
                locationRepository.setActiveLocationId(existing.id)
                refreshLocation(existing.id)
            } else {
                attemptAddCurrentLocation()
            }
        }
    }

    private suspend fun attemptAddCurrentLocation() {
        when (val located = locationSource.getCurrentLocation()) {
            is LocationResult.Success -> {
                val loc = Location(AppPreferences.CURRENT_LOCATION_ID, located.lat, located.lon, "Current location", null, isCurrentLocation = true)
                when (locationRepository.addLocation(loc)) {
                    LocationRepository.AddResult.CapReached -> _locationLimitMessage.value = capReachedMessage()
                    else -> refreshLocation(loc.id)
                }
            }
            LocationResult.PermissionDenied, LocationResult.Unavailable -> {
                // No saved locations and no GPS — MainScreen shows its "add a location" prompt
                // for an empty locations list; nothing further to do here.
            }
        }
    }

    private var searchJob: Job? = null

    // Cancels any in-flight search before starting a new one — without this, a stale response for
    // an earlier keystroke's partial query can arrive after a later, more complete query's
    // response and overwrite it with outdated results.
    fun search(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _searchResults.value = geocodingApi.search(query)
        }
    }

    // Saves a search result as a new location (was named selectLocation() in the single-location
    // version — renamed because selectLocation() now means "jump to an already-saved location").
    fun addLocationFromSearch(result: GeocodingResult) {
        viewModelScope.launch {
            val location = result.toSavedLocation()
            _searchResults.value = emptyList()
            when (locationRepository.addLocation(location)) {
                LocationRepository.AddResult.CapReached -> _locationLimitMessage.value = capReachedMessage()
                else -> refreshLocation(location.id)
            }
        }
    }

    fun removeLocation(id: String) {
        viewModelScope.launch {
            locationRepository.removeLocation(id)
            weatherRepository.evictCache(id)
            _weatherStates.update { it - id }
        }
    }

    fun reorderLocations(orderedIds: List<String>) {
        viewModelScope.launch { locationRepository.reorder(orderedIds) }
    }

    // Jumps the pager to an already-saved location (picker row tap) — does NOT save anything new.
    fun selectLocation(id: String) {
        viewModelScope.launch { locationRepository.setActiveLocationId(id) }
        refreshLocation(id)
    }

    fun dismissLocationLimitMessage() {
        _locationLimitMessage.value = null
    }

    private fun capReachedMessage() =
        "You've reached the ${AppPreferences.MAX_SAVED_LOCATIONS}-location limit — remove one to add another."
}
