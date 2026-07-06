package com.fossisawesome.ventus.data.repository

import com.fossisawesome.ventus.data.model.Location
import com.fossisawesome.ventus.data.storage.AppPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class LocationRepository(private val prefs: AppPreferences) {

    enum class AddResult { Added, AlreadyExists, CapReached }

    private val gson = Gson()
    private val listType = object : TypeToken<List<Location>>() {}.type

    val locationsFlow: Flow<List<Location>> = prefs.savedLocationsJson.map { json ->
        if (json == null) emptyList() else runCatching { gson.fromJson<List<Location>>(json, listType) }.getOrDefault(emptyList())
    }
    val activeLocationIdFlow: Flow<String?> = prefs.activeLocationId

    // One-time migration from the 3 old scalar keys (LOCATION_LAT/LON/NAME) into the new
    // JSON-list schema. Safe to call on every app start: it's a no-op (returns null) once the new
    // key has ANY value, including an explicitly-empty list written by a prior no-op call. Returns
    // the migrated Location so the caller (WeatherViewModel.loadInitial) can also carry the old
    // single-slot weather cache forward via WeatherRepository.migrateLegacyCache().
    suspend fun migrateIfNeeded(): Location? {
        if (prefs.savedLocationsJson.first() != null) return null

        val lat = prefs.locationLat.first()
        val lon = prefs.locationLon.first()
        val name = prefs.locationName.first()
        if (lat == null || lon == null || name == null) {
            prefs.setSavedLocationsJson(gson.toJson(emptyList<Location>()))
            return null
        }

        val isCurrent = name == "Current location"
        val migrated = Location(
            id = if (isCurrent) AppPreferences.CURRENT_LOCATION_ID else "legacy:${lat}_${lon}",
            lat = lat, lon = lon, name = name, country = null, isCurrentLocation = isCurrent,
        )
        prefs.setSavedLocationsJson(gson.toJson(listOf(migrated)))
        prefs.setActiveLocationId(migrated.id)
        prefs.clearLocation()
        return migrated
    }

    suspend fun addLocation(location: Location): AddResult {
        val current = locationsFlow.first()
        if (current.any { it.id == location.id }) {
            setActiveLocationId(location.id)
            return AddResult.AlreadyExists
        }
        if (current.size >= AppPreferences.MAX_SAVED_LOCATIONS) return AddResult.CapReached
        prefs.setSavedLocationsJson(gson.toJson(current + location))
        setActiveLocationId(location.id)
        return AddResult.Added
    }

    suspend fun removeLocation(id: String) {
        val current = locationsFlow.first()
        val remaining = current.filterNot { it.id == id }
        prefs.setSavedLocationsJson(gson.toJson(remaining))
        if (activeLocationIdFlow.first() == id) {
            remaining.firstOrNull()?.let { setActiveLocationId(it.id) }
        }
    }

    suspend fun reorder(orderedIds: List<String>) {
        val byId = locationsFlow.first().associateBy { it.id }
        val reordered = orderedIds.mapNotNull { byId[it] }
        prefs.setSavedLocationsJson(gson.toJson(reordered))
    }

    suspend fun setActiveLocationId(id: String) = prefs.setActiveLocationId(id)

    // GPS-tracked entries re-resolve their coordinates on every refresh (see WeatherViewModel) —
    // this persists the freshest fix as the new "last known" fallback for the next time GPS itself
    // is unavailable, without touching any other saved location.
    suspend fun upsertCurrentLocationCoords(lat: Double, lon: Double) {
        val current = locationsFlow.first()
        val updated = current.map { if (it.isCurrentLocation) it.copy(lat = lat, lon = lon) else it }
        prefs.setSavedLocationsJson(gson.toJson(updated))
    }
}
