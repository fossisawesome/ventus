package com.fossisawesome.ventus.data.repository

import com.fossisawesome.ventus.data.Units
import com.fossisawesome.ventus.data.api.AirQualityApi
import com.fossisawesome.ventus.data.api.WeatherApi
import com.fossisawesome.ventus.data.isUsLocation
import com.fossisawesome.ventus.data.model.WeatherSnapshot
import com.fossisawesome.ventus.data.model.WeatherUiState
import com.fossisawesome.ventus.data.storage.AppPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.first

class WeatherRepository(
    private val openMeteoApi: WeatherApi,
    private val nwsApi: WeatherApi,
    private val airQualityApi: AirQualityApi,
    private val prefs: AppPreferences,
) {
    private val gson = Gson()
    private val cacheMapType = object : TypeToken<Map<String, CachedEntry>>() {}.type

    // One entry per saved location, keyed by Location.id — replaces the old single unkeyed
    // CACHED_WEATHER_JSON slot, which silently returned a DIFFERENT location's stale snapshot as
    // "Stale" on a fetch failure whenever the previously-cached location didn't match the one just
    // requested. A cache miss for the SPECIFIC requested location is now a real Error.
    private data class CachedEntry(val snapshot: WeatherSnapshot, val fetchedAt: Long)

    suspend fun refresh(locationId: String, lat: Double, lon: Double, locationName: String, units: Units): WeatherUiState {
        return try {
            val provider = prefs.weatherProvider.first()
            // NWS has no non-US coverage. Unlike the single-location version, this does NOT
            // reset the stored global weatherProvider preference — with multiple saved locations
            // possibly spanning US and non-US, silently falling back to Open-Meteo for THIS page's
            // refresh (while leaving "nws" intact for the user's US pages) is the only sane
            // behavior; SettingsViewModel.isNwsAvailable reflects per-page coverage reactively, so
            // the Settings toggle greys out correctly with no global mutation.
            val useNws = provider == "nws" && isUsLocation(lat, lon)

            val aqi = try {
                airQualityApi.fetchAqi(lat, lon)
            } catch (_: Exception) {
                null
            }
            val api = if (useNws) nwsApi else openMeteoApi
            val snapshot = api.fetchForecast(locationName, units, lat, lon, aqi)
            val now = System.currentTimeMillis()
            writeCache(locationId, snapshot, now)
            WeatherUiState.Success(snapshot)
        } catch (e: Exception) {
            val cached = readCache(locationId)
            if (cached != null) {
                WeatherUiState.Stale(cached.snapshot, cached.fetchedAt)
            } else {
                WeatherUiState.Error(e.message ?: "Couldn't load weather")
            }
        }
    }

    suspend fun loadCached(locationId: String): WeatherUiState {
        val cached = readCache(locationId) ?: return WeatherUiState.NeedsLocation
        return WeatherUiState.Stale(cached.snapshot, cached.fetchedAt)
    }

    suspend fun evictCache(locationId: String) {
        val map = readCacheMap().toMutableMap()
        map.remove(locationId)
        prefs.setCachedWeatherByLocationJson(gson.toJson(map))
    }

    // Copies the old single-slot cache forward into the new per-location map under the given
    // (migrated) location id, so the user doesn't see a blank Loading screen for their
    // already-cached city right after upgrading. Called once by WeatherViewModel.loadInitial()
    // when LocationRepository.migrateIfNeeded() reports a migration happened; a no-op if there's
    // nothing to migrate.
    suspend fun migrateLegacyCache(locationId: String) {
        val json = prefs.cachedWeatherJson.first() ?: return
        val fetchedAt = prefs.cachedWeatherFetchedAt.first()
        val snapshot = try {
            gson.fromJson(json, WeatherSnapshot::class.java)
        } catch (_: Exception) {
            null
        } ?: return
        writeCache(locationId, snapshot, fetchedAt)
        prefs.clearLegacyWeatherCache()
    }

    private suspend fun writeCache(locationId: String, snapshot: WeatherSnapshot, fetchedAt: Long) {
        val map = readCacheMap().toMutableMap()
        map[locationId] = CachedEntry(snapshot, fetchedAt)
        prefs.setCachedWeatherByLocationJson(gson.toJson(map))
    }

    private suspend fun readCache(locationId: String): CachedEntry? = readCacheMap()[locationId]

    private suspend fun readCacheMap(): Map<String, CachedEntry> {
        val json = prefs.cachedWeatherByLocationJson.first() ?: return emptyMap()
        return try {
            gson.fromJson(json, cacheMapType)
        } catch (_: Exception) {
            emptyMap()
        }
    }
}
