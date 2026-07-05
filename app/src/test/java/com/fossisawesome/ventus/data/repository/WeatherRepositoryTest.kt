package com.fossisawesome.ventus.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.fossisawesome.ventus.data.Units
import com.fossisawesome.ventus.data.model.*
import com.fossisawesome.ventus.data.storage.AppPreferences
import com.fossisawesome.ventus.data.api.AirQualityApi
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
    private val name: String,
    private val shouldFail: Boolean = false,
) : WeatherApi {
    var callCount = 0
        private set

    override suspend fun fetchForecast(locationName: String, units: Units, lat: Double, lon: Double, aqi: Int?): WeatherSnapshot {
        callCount++
        if (shouldFail) error("network down")
        return sampleSnapshot(locationName)
    }
}

private class FakeAirQualityApi(private val aqi: Int? = null) : AirQualityApi {
    override suspend fun fetchAqi(lat: Double, lon: Double): Int? = aqi
}

private fun sampleSnapshot(name: String) = WeatherSnapshot(
    locationName = name,
    units = Units.METRIC,
    currentTempC = 22.5,
    currentApparentTempC = 23.0,
    currentHumidity = 60,
    currentWindKmh = 12.3,
    currentWeatherCode = 2,
    hourly = listOf(HourlyPoint(epochSeconds = 0L, tempC = 22.5, precipitationProbability = 10, weatherCode = 2)),
    daily = listOf(DailyPoint(epochSeconds = 0L, tempMaxC = 25.0, tempMinC = 15.0, weatherCode = 2)),
)

class WeatherRepositoryTest {

    @Test
    fun `refresh returns Success and caches on API success`() = runBlocking {
        val prefs = AppPreferences(FakeDataStore())
        val repo = WeatherRepository(FakeWeatherApi("open-meteo"), FakeWeatherApi("nws"), FakeAirQualityApi(), prefs)

        val state = repo.refresh(40.7128, -74.0060, "New York", Units.METRIC)

        assertTrue(state is WeatherUiState.Success)
        val snapshot = (state as WeatherUiState.Success).snapshot
        assertEquals("New York", snapshot.locationName)
        assertEquals(22.5, snapshot.currentTempC, 0.001)

        val cachedJson = prefs.cachedWeatherJson.firstOrNull()
        assertTrue(cachedJson != null && cachedJson.contains("New York"))
    }

    @Test
    fun `refresh falls back to cache as Stale on API failure`() = runBlocking {
        val prefs = AppPreferences(FakeDataStore())
        prefs.setCachedWeather(Gson().toJson(sampleSnapshot("Cached City")), fetchedAt = 1000L)

        val repo = WeatherRepository(FakeWeatherApi("open-meteo", shouldFail = true), FakeWeatherApi("nws"), FakeAirQualityApi(), prefs)
        val state = repo.refresh(0.0, 0.0, "New Location", Units.METRIC)

        assertTrue(state is WeatherUiState.Stale)
        val stale = state as WeatherUiState.Stale
        assertEquals("Cached City", stale.snapshot.locationName)
        assertEquals(1000L, stale.fetchedAt)
    }

    @Test
    fun `refresh returns Error on API failure with no cache`() = runBlocking {
        val prefs = AppPreferences(FakeDataStore())
        val repo = WeatherRepository(FakeWeatherApi("open-meteo", shouldFail = true), FakeWeatherApi("nws"), FakeAirQualityApi(), prefs)

        val state = repo.refresh(0.0, 0.0, "Nowhere", Units.METRIC)

        assertTrue(state is WeatherUiState.Error)
    }

    @Test
    fun `loadCached returns Stale when a cache entry exists`() = runBlocking {
        val prefs = AppPreferences(FakeDataStore())
        prefs.setCachedWeather(Gson().toJson(sampleSnapshot("Cached City")), fetchedAt = 2000L)

        val repo = WeatherRepository(FakeWeatherApi("open-meteo"), FakeWeatherApi("nws"), FakeAirQualityApi(), prefs)
        val state = repo.loadCached()

        assertTrue(state is WeatherUiState.Stale)
        assertEquals(2000L, (state as WeatherUiState.Stale).fetchedAt)
    }

    @Test
    fun `loadCached returns NeedsLocation when no cache exists`() = runBlocking {
        val prefs = AppPreferences(FakeDataStore())
        val repo = WeatherRepository(FakeWeatherApi("open-meteo"), FakeWeatherApi("nws"), FakeAirQualityApi(), prefs)

        val state = repo.loadCached()

        assertTrue(state is WeatherUiState.NeedsLocation)
    }

    @Test
    fun `refresh uses NWS api when provider is nws and location is in the US`() = runBlocking {
        val prefs = AppPreferences(FakeDataStore())
        prefs.setWeatherProvider("nws")
        val openMeteo = FakeWeatherApi("open-meteo")
        val nws = FakeWeatherApi("nws")
        val repo = WeatherRepository(openMeteo, nws, FakeAirQualityApi(), prefs)

        repo.refresh(40.7128, -74.0060, "New York", Units.METRIC) // US coordinates

        assertEquals(0, openMeteo.callCount)
        assertEquals(1, nws.callCount)
    }

    @Test
    fun `refresh falls back to open-meteo and resets preference when nws selected outside the US`() = runBlocking {
        val prefs = AppPreferences(FakeDataStore())
        prefs.setWeatherProvider("nws")
        val openMeteo = FakeWeatherApi("open-meteo")
        val nws = FakeWeatherApi("nws")
        val repo = WeatherRepository(openMeteo, nws, FakeAirQualityApi(), prefs)

        repo.refresh(51.5072, -0.1276, "London", Units.METRIC) // non-US coordinates

        assertEquals(1, openMeteo.callCount)
        assertEquals(0, nws.callCount)
        assertEquals("open-meteo", prefs.weatherProvider.firstOrNull())
    }
}
