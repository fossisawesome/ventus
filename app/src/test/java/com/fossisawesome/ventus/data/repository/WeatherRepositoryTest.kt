package com.fossisawesome.ventus.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.fossisawesome.ventus.data.Units
import com.fossisawesome.ventus.data.model.*
import com.fossisawesome.ventus.data.storage.AppPreferences
import com.fossisawesome.ventus.data.api.WeatherApi
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// In-memory DataStore fake — avoids needing an Android Context/Robolectric for these tests.
private class FakeDataStore : DataStore<Preferences> {
    private val flow = MutableStateFlow<Preferences>(emptyPreferences())
    override val data = flow
    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
        val updated = transform(flow.value)
        flow.value = updated
        return updated
    }
}

private class FakeWeatherApi(
    private val response: OpenMeteoForecastResponse? = null,
    private val shouldFail: Boolean = false,
) : WeatherApi {
    override suspend fun fetchForecast(lat: Double, lon: Double): OpenMeteoForecastResponse {
        if (shouldFail) error("network down")
        return response ?: error("no fixture response configured")
    }
}

private val sampleResponse = OpenMeteoForecastResponse(
    latitude = 40.7128,
    longitude = -74.0060,
    current = CurrentBlock("2026-07-05T14:00", 22.5, 60, 23.0, 2, 12.3),
    hourly = HourlyBlock(listOf("2026-07-05T14:00"), listOf(22.5), listOf(10), listOf(2)),
    daily = DailyBlock(listOf("2026-07-05"), listOf(2), listOf(25.0), listOf(15.0)),
)

class WeatherRepositoryTest {

    @Test
    fun `refresh returns Success and caches on API success`() = runBlocking {
        val prefs = AppPreferences(FakeDataStore())
        val repo = WeatherRepository(FakeWeatherApi(response = sampleResponse), prefs)

        val state = repo.refresh(40.7128, -74.0060, "New York", Units.METRIC)

        assertTrue(state is WeatherUiState.Success)
        val snapshot = (state as WeatherUiState.Success).snapshot
        assertEquals("New York", snapshot.locationName)
        assertEquals(22.5, snapshot.currentTempC, 0.001)

        // Cache was written.
        val cachedJson = prefs.cachedWeatherJson.firstOrNull()
        assertTrue(cachedJson != null && cachedJson.contains("New York"))
    }

    @Test
    fun `refresh falls back to cache as Stale on API failure`() = runBlocking {
        val prefs = AppPreferences(FakeDataStore())
        val cachedSnapshot = mapForecastResponseForTest("Cached City")
        prefs.setCachedWeather(Gson().toJson(cachedSnapshot), fetchedAt = 1000L)

        val repo = WeatherRepository(FakeWeatherApi(shouldFail = true), prefs)
        val state = repo.refresh(0.0, 0.0, "New Location", Units.METRIC)

        assertTrue(state is WeatherUiState.Stale)
        val stale = state as WeatherUiState.Stale
        assertEquals("Cached City", stale.snapshot.locationName)
        assertEquals(1000L, stale.fetchedAt)
    }

    @Test
    fun `refresh returns Error on API failure with no cache`() = runBlocking {
        val prefs = AppPreferences(FakeDataStore())
        val repo = WeatherRepository(FakeWeatherApi(shouldFail = true), prefs)

        val state = repo.refresh(0.0, 0.0, "Nowhere", Units.METRIC)

        assertTrue(state is WeatherUiState.Error)
    }

    @Test
    fun `loadCached returns Stale when a cache entry exists`() = runBlocking {
        val prefs = AppPreferences(FakeDataStore())
        val cachedSnapshot = mapForecastResponseForTest("Cached City")
        prefs.setCachedWeather(Gson().toJson(cachedSnapshot), fetchedAt = 2000L)

        val repo = WeatherRepository(FakeWeatherApi(), prefs)
        val state = repo.loadCached()

        assertTrue(state is WeatherUiState.Stale)
        assertEquals(2000L, (state as WeatherUiState.Stale).fetchedAt)
    }

    @Test
    fun `loadCached returns NeedsLocation when no cache exists`() = runBlocking {
        val prefs = AppPreferences(FakeDataStore())
        val repo = WeatherRepository(FakeWeatherApi(), prefs)

        val state = repo.loadCached()

        assertTrue(state is WeatherUiState.NeedsLocation)
    }
}

private fun mapForecastResponseForTest(name: String) =
    com.fossisawesome.ventus.data.api.mapForecastResponse(name, Units.METRIC, sampleResponse)
