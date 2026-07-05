package com.fossisawesome.ventus.data.repository

import com.fossisawesome.ventus.data.Units
import com.fossisawesome.ventus.data.api.AirQualityApi
import com.fossisawesome.ventus.data.api.WeatherApi
import com.fossisawesome.ventus.data.api.mapForecastResponse
import com.fossisawesome.ventus.data.model.WeatherSnapshot
import com.fossisawesome.ventus.data.model.WeatherUiState
import com.fossisawesome.ventus.data.storage.AppPreferences
import com.google.gson.Gson
import kotlinx.coroutines.flow.first

class WeatherRepository(
    private val api: WeatherApi,
    private val airQualityApi: AirQualityApi,
    private val prefs: AppPreferences,
) {
    private val gson = Gson()

    suspend fun refresh(lat: Double, lon: Double, locationName: String, units: Units): WeatherUiState {
        return try {
            val response = api.fetchForecast(lat, lon)
            val aqi = try {
                airQualityApi.fetchAqi(lat, lon)
            } catch (_: Exception) {
                null
            }
            val snapshot = mapForecastResponse(locationName, units, response, aqi)
            val now = System.currentTimeMillis()
            prefs.setCachedWeather(gson.toJson(snapshot), now)
            WeatherUiState.Success(snapshot)
        } catch (e: Exception) {
            val cached = readCache()
            if (cached != null) {
                WeatherUiState.Stale(cached.first, cached.second)
            } else {
                WeatherUiState.Error(e.message ?: "Couldn't load weather")
            }
        }
    }

    suspend fun loadCached(): WeatherUiState {
        val cached = readCache() ?: return WeatherUiState.NeedsLocation
        return WeatherUiState.Stale(cached.first, cached.second)
    }

    private suspend fun readCache(): Pair<WeatherSnapshot, Long>? {
        val json = prefs.cachedWeatherJson.first() ?: return null
        val fetchedAt = prefs.cachedWeatherFetchedAt.first()
        val snapshot = try {
            gson.fromJson(json, WeatherSnapshot::class.java)
        } catch (_: Exception) {
            null
        } ?: return null
        return snapshot to fetchedAt
    }
}
