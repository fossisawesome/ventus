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
        val LOCATION_LAT = doublePreferencesKey("location_lat")
        val LOCATION_LON = doublePreferencesKey("location_lon")
        val LOCATION_NAME = stringPreferencesKey("location_name")
        val CACHED_WEATHER_JSON = stringPreferencesKey("cached_weather_json")
        val CACHED_WEATHER_FETCHED_AT = longPreferencesKey("cached_weather_fetched_at")
    }

    val themeId: Flow<String> = store.data.map { it[THEME_ID] ?: "ventus" }
    val fontFamily: Flow<String> = store.data.map { it[FONT_FAMILY] ?: "Liberation Mono" }
    val unitsMode: Flow<String> = store.data.map { it[UNITS_MODE] ?: "auto" }
    val locationLat: Flow<Double?> = store.data.map { it[LOCATION_LAT] }
    val locationLon: Flow<Double?> = store.data.map { it[LOCATION_LON] }
    val locationName: Flow<String?> = store.data.map { it[LOCATION_NAME] }
    val cachedWeatherJson: Flow<String?> = store.data.map { it[CACHED_WEATHER_JSON] }
    val cachedWeatherFetchedAt: Flow<Long> = store.data.map { it[CACHED_WEATHER_FETCHED_AT] ?: 0L }

    suspend fun setThemeId(id: String) = store.edit { it[THEME_ID] = id }
    suspend fun setFontFamily(name: String) = store.edit { it[FONT_FAMILY] = name }
    suspend fun setUnitsMode(mode: String) = store.edit { it[UNITS_MODE] = mode }

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
}
