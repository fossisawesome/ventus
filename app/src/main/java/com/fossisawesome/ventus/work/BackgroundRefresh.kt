package com.fossisawesome.ventus.work

import com.fossisawesome.ventus.data.location.LocationSource
import com.fossisawesome.ventus.data.location.resolveLocationCoords
import com.fossisawesome.ventus.data.repository.LocationRepository
import com.fossisawesome.ventus.data.repository.WeatherRepository
import com.fossisawesome.ventus.data.resolveUnits
import com.fossisawesome.ventus.data.storage.AppPreferences
import kotlinx.coroutines.flow.first

// Refreshes EVERY saved location's cache, not just the active one, so a future widget (or just
// swiping to a location that hasn't been viewed in a while) shows current data rather than
// stale-until-viewed. Deliberately takes plain constructor-injected dependencies (no Context, no
// WorkManager types) so it's fully unit-testable with the same fakes used elsewhere in this repo —
// WeatherRefreshWorker is a thin wrapper that calls this from doWork().
suspend fun refreshAllLocations(
    weatherRepository: WeatherRepository,
    locationRepository: LocationRepository,
    locationSource: LocationSource,
    prefs: AppPreferences,
    countryCode: String,
) {
    val locations = locationRepository.locationsFlow.first()
    val units = resolveUnits(prefs.unitsMode.first(), countryCode)
    locations.forEach { location ->
        val (lat, lon) = resolveLocationCoords(location, locationSource)
        weatherRepository.refresh(location.id, lat, lon, location.name, units)
        if (location.isCurrentLocation) locationRepository.upsertCurrentLocationCoords(lat, lon)
    }
}
