package com.fossisawesome.ventus.data.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("ventus_prefs")

// Non-sensitive app preferences stored in DataStore.
// The primary constructor takes the DataStore directly (rather than only a Context) so unit
// tests can inject an in-memory fake instead of a real, file-backed one, without needing
// Robolectric or an Android Context.
class AppPreferences(private val store: DataStore<Preferences>) {

    constructor(context: Context) : this(context.dataStore)

    companion object {
        val THEME_ID = stringPreferencesKey("theme_id")
        val FONT_FAMILY = stringPreferencesKey("font_family")
        // "auto" | "metric" | "imperial"
        val UNITS_MODE = stringPreferencesKey("units_mode")
        // "open-meteo" | "nws"
        val WEATHER_PROVIDER = stringPreferencesKey("weather_provider")
        val LOCATION_LAT = doublePreferencesKey("location_lat")
        val LOCATION_LON = doublePreferencesKey("location_lon")
        val LOCATION_NAME = stringPreferencesKey("location_name")
        val CACHED_WEATHER_JSON = stringPreferencesKey("cached_weather_json")
        val CACHED_WEATHER_FETCHED_AT = longPreferencesKey("cached_weather_fetched_at")

        // Gson-serialized List<Location> (see data/model/Location.kt) — replaces LOCATION_LAT/
        // LOCATION_LON/LOCATION_NAME, which are kept above ONLY so LocationRepository can migrate
        // existing installs' single saved location into this list on first read.
        val SAVED_LOCATIONS_JSON = stringPreferencesKey("saved_locations_json")
        val ACTIVE_LOCATION_ID = stringPreferencesKey("active_location_id")
        // Gson-serialized Map<locationId, CachedEntry> — replaces the single-slot
        // CACHED_WEATHER_JSON/CACHED_WEATHER_FETCHED_AT pair, which silently returned a
        // DIFFERENT location's stale snapshot as "Stale" on a fetch failure whenever the
        // previously-cached location didn't match the one just requested.
        val CACHED_WEATHER_BY_LOCATION_JSON = stringPreferencesKey("cached_weather_by_location_json")

        const val MAX_SAVED_LOCATIONS = 10
        const val CURRENT_LOCATION_ID = "current-location"

        val BACKGROUND_REFRESH_ENABLED = booleanPreferencesKey("background_refresh_enabled")
        val BACKGROUND_REFRESH_INTERVAL_MINUTES = intPreferencesKey("background_refresh_interval_minutes")
    }

    val themeId: Flow<String> = store.data.map { it[THEME_ID] ?: "ventus" }
    val fontFamily: Flow<String> = store.data.map { it[FONT_FAMILY] ?: "Liberation Mono" }
    val unitsMode: Flow<String> = store.data.map { it[UNITS_MODE] ?: "auto" }
    val weatherProvider: Flow<String> = store.data.map { it[WEATHER_PROVIDER] ?: "open-meteo" }
    val locationLat: Flow<Double?> = store.data.map { it[LOCATION_LAT] }
    val locationLon: Flow<Double?> = store.data.map { it[LOCATION_LON] }
    val locationName: Flow<String?> = store.data.map { it[LOCATION_NAME] }
    val cachedWeatherJson: Flow<String?> = store.data.map { it[CACHED_WEATHER_JSON] }
    val cachedWeatherFetchedAt: Flow<Long> = store.data.map { it[CACHED_WEATHER_FETCHED_AT] ?: 0L }
    val savedLocationsJson: Flow<String?> = store.data.map { it[SAVED_LOCATIONS_JSON] }
    val activeLocationId: Flow<String?> = store.data.map { it[ACTIVE_LOCATION_ID] }
    val cachedWeatherByLocationJson: Flow<String?> = store.data.map { it[CACHED_WEATHER_BY_LOCATION_JSON] }
    val backgroundRefreshEnabled: Flow<Boolean> = store.data.map { it[BACKGROUND_REFRESH_ENABLED] ?: false }
    val backgroundRefreshIntervalMinutes: Flow<Int> = store.data.map { it[BACKGROUND_REFRESH_INTERVAL_MINUTES] ?: 60 }

    suspend fun setThemeId(id: String) = store.edit { it[THEME_ID] = id }
    suspend fun setFontFamily(name: String) = store.edit { it[FONT_FAMILY] = name }
    suspend fun setUnitsMode(mode: String) = store.edit { it[UNITS_MODE] = mode }
    suspend fun setWeatherProvider(id: String) = store.edit { it[WEATHER_PROVIDER] = id }

    suspend fun setLocation(lat: Double, lon: Double, name: String) = store.edit {
        it[LOCATION_LAT] = lat
        it[LOCATION_LON] = lon
        it[LOCATION_NAME] = name
    }

    suspend fun clearLocation() = store.edit {
        it.remove(LOCATION_LAT)
        it.remove(LOCATION_LON)
        it.remove(LOCATION_NAME)
    }

    suspend fun setCachedWeather(json: String, fetchedAt: Long) = store.edit {
        it[CACHED_WEATHER_JSON] = json
        it[CACHED_WEATHER_FETCHED_AT] = fetchedAt
    }

    suspend fun setSavedLocationsJson(json: String) = store.edit { it[SAVED_LOCATIONS_JSON] = json }
    suspend fun setActiveLocationId(id: String) = store.edit { it[ACTIVE_LOCATION_ID] = id }
    suspend fun setCachedWeatherByLocationJson(json: String) = store.edit { it[CACHED_WEATHER_BY_LOCATION_JSON] = json }

    // Removes the pre-multi-location cache slot once its contents have been migrated forward into
    // CACHED_WEATHER_BY_LOCATION_JSON — see LocationRepository.migrateIfNeeded().
    suspend fun clearLegacyWeatherCache() = store.edit {
        it.remove(CACHED_WEATHER_JSON)
        it.remove(CACHED_WEATHER_FETCHED_AT)
    }

    suspend fun setBackgroundRefreshEnabled(enabled: Boolean) = store.edit { it[BACKGROUND_REFRESH_ENABLED] = enabled }
    suspend fun setBackgroundRefreshIntervalMinutes(minutes: Int) = store.edit { it[BACKGROUND_REFRESH_INTERVAL_MINUTES] = minutes }
}
