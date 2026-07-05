package com.fossisawesome.ventus.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fossisawesome.ventus.data.api.GeocodingApi
import com.fossisawesome.ventus.data.location.LocationResult
import com.fossisawesome.ventus.data.location.LocationSource
import com.fossisawesome.ventus.data.model.GeocodingResult
import com.fossisawesome.ventus.data.model.WeatherUiState
import com.fossisawesome.ventus.data.repository.WeatherRepository
import com.fossisawesome.ventus.data.resolveUnits
import com.fossisawesome.ventus.data.storage.AppPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class WeatherViewModel(
    private val repository: WeatherRepository,
    private val locationSource: LocationSource,
    private val geocodingApi: GeocodingApi,
    private val prefs: AppPreferences,
    private val countryCode: String,
) : ViewModel() {

    private val _state = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading)
    val state: StateFlow<WeatherUiState> = _state.asStateFlow()

    private val _searchResults = MutableStateFlow<List<GeocodingResult>>(emptyList())
    val searchResults: StateFlow<List<GeocodingResult>> = _searchResults.asStateFlow()

    // On first load: prefer a previously saved location, otherwise try GPS, otherwise fall
    // back to any cached weather, otherwise ask the user to search.
    fun loadInitial() {
        viewModelScope.launch {
            val savedLat = prefs.locationLat.first()
            val savedLon = prefs.locationLon.first()
            val savedName = prefs.locationName.first()

            if (savedLat != null && savedLon != null && savedName != null) {
                fetchWeather(savedLat, savedLon, savedName)
                return@launch
            }

            when (val located = locationSource.getCurrentLocation()) {
                is LocationResult.Success -> {
                    prefs.setLocation(located.lat, located.lon, "Current location")
                    fetchWeather(located.lat, located.lon, "Current location")
                }
                LocationResult.PermissionDenied, LocationResult.Unavailable -> {
                    _state.value = repository.loadCached()
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val lat = prefs.locationLat.first()
            val lon = prefs.locationLon.first()
            val name = prefs.locationName.first()
            if (lat != null && lon != null && name != null) {
                fetchWeather(lat, lon, name)
            }
        }
    }

    fun useCurrentLocation() {
        viewModelScope.launch {
            when (val located = locationSource.getCurrentLocation()) {
                is LocationResult.Success -> {
                    prefs.setLocation(located.lat, located.lon, "Current location")
                    fetchWeather(located.lat, located.lon, "Current location")
                }
                LocationResult.PermissionDenied, LocationResult.Unavailable -> {
                    _state.value = WeatherUiState.Error("Location unavailable — try searching for your city")
                }
            }
        }
    }

    fun search(query: String) {
        viewModelScope.launch {
            _searchResults.value = geocodingApi.search(query)
        }
    }

    fun selectLocation(result: GeocodingResult) {
        viewModelScope.launch {
            val name = listOfNotNull(result.name, result.country).joinToString(", ")
            prefs.setLocation(result.latitude, result.longitude, name)
            _searchResults.value = emptyList()
            fetchWeather(result.latitude, result.longitude, name)
        }
    }

    private suspend fun fetchWeather(lat: Double, lon: Double, name: String) {
        _state.value = WeatherUiState.Loading
        val unitsMode = prefs.unitsMode.first()
        val units = resolveUnits(unitsMode, countryCode)
        _state.value = repository.refresh(lat, lon, name, units)
    }
}
