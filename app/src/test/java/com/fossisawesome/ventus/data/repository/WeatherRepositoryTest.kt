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
    fun `refresh returns Success and caches under the given location id`() = runBlocking {
        val prefs = AppPreferences(FakeDataStore())
        val repo = WeatherRepository(FakeWeatherApi("open-meteo"), FakeWeatherApi("nws"), FakeAirQualityApi(), prefs)

        val state = repo.refresh("loc-ny", 40.7128, -74.0060, "New York", Units.METRIC)

        assertTrue(state is WeatherUiState.Success)
        val snapshot = (state as WeatherUiState.Success).snapshot
        assertEquals("New York", snapshot.locationName)

        val cachedJson = prefs.cachedWeatherByLocationJson.firstOrNull()
        assertTrue(cachedJson != null && cachedJson.contains("loc-ny") && cachedJson.contains("New York"))
    }

    @Test
    fun `refresh falls back to that location's own cache as Stale on API failure`() = runBlocking {
        val prefs = AppPreferences(FakeDataStore())
        val repo = WeatherRepository(FakeWeatherApi("open-meteo"), FakeWeatherApi("nws"), FakeAirQualityApi(), prefs)
        repo.refresh("loc-a", 1.0, 1.0, "Cached City", Units.METRIC) // seeds the cache for loc-a

        val failing = WeatherRepository(FakeWeatherApi("open-meteo", shouldFail = true), FakeWeatherApi("nws"), FakeAirQualityApi(), prefs)
        val state = failing.refresh("loc-a", 1.0, 1.0, "Cached City", Units.METRIC)

        assertTrue(state is WeatherUiState.Stale)
        assertEquals("Cached City", (state as WeatherUiState.Stale).snapshot.locationName)
    }

    @Test
    fun `refresh returns Error, not another location's stale cache, on a cache miss for this location`() = runBlocking {
        // Regression test for the old single-slot cache bug: a fetch failure for "loc-b" must
        // NEVER resolve to "loc-a"'s cached snapshot just because it's the only thing cached.
        val prefs = AppPreferences(FakeDataStore())
        val seed = WeatherRepository(FakeWeatherApi("open-meteo"), FakeWeatherApi("nws"), FakeAirQualityApi(), prefs)
        seed.refresh("loc-a", 1.0, 1.0, "Location A", Units.METRIC)

        val failing = WeatherRepository(FakeWeatherApi("open-meteo", shouldFail = true), FakeWeatherApi("nws"), FakeAirQualityApi(), prefs)
        val state = failing.refresh("loc-b", 2.0, 2.0, "Location B", Units.METRIC)

        assertTrue(state is WeatherUiState.Error)
    }

    @Test
    fun `refresh keeps independent caches per location`() = runBlocking {
        val prefs = AppPreferences(FakeDataStore())
        val repo = WeatherRepository(FakeWeatherApi("open-meteo"), FakeWeatherApi("nws"), FakeAirQualityApi(), prefs)
        repo.refresh("loc-a", 1.0, 1.0, "Location A", Units.METRIC)
        repo.refresh("loc-b", 2.0, 2.0, "Location B", Units.METRIC)

        assertTrue((repo.loadCached("loc-a") as WeatherUiState.Stale).snapshot.locationName == "Location A")
        assertTrue((repo.loadCached("loc-b") as WeatherUiState.Stale).snapshot.locationName == "Location B")
    }

    @Test
    fun `loadCached returns NeedsLocation when no cache exists for that id`() = runBlocking {
        val prefs = AppPreferences(FakeDataStore())
        val repo = WeatherRepository(FakeWeatherApi("open-meteo"), FakeWeatherApi("nws"), FakeAirQualityApi(), prefs)

        assertTrue(repo.loadCached("nowhere") is WeatherUiState.NeedsLocation)
    }

    @Test
    fun `evictCache removes only the given location's entry`() = runBlocking {
        val prefs = AppPreferences(FakeDataStore())
        val repo = WeatherRepository(FakeWeatherApi("open-meteo"), FakeWeatherApi("nws"), FakeAirQualityApi(), prefs)
        repo.refresh("loc-a", 1.0, 1.0, "Location A", Units.METRIC)
        repo.refresh("loc-b", 2.0, 2.0, "Location B", Units.METRIC)

        repo.evictCache("loc-a")

        assertTrue(repo.loadCached("loc-a") is WeatherUiState.NeedsLocation)
        assertTrue(repo.loadCached("loc-b") is WeatherUiState.Stale)
    }

    @Test
    fun `migrateLegacyCache copies the old single-slot cache into the new keyed map`() = runBlocking {
        val prefs = AppPreferences(FakeDataStore())
        prefs.setCachedWeather(Gson().toJson(sampleSnapshot("Legacy City")), fetchedAt = 1234L)
        val repo = WeatherRepository(FakeWeatherApi("open-meteo"), FakeWeatherApi("nws"), FakeAirQualityApi(), prefs)

        repo.migrateLegacyCache("migrated-id")

        val state = repo.loadCached("migrated-id")
        assertTrue(state is WeatherUiState.Stale)
        assertEquals("Legacy City", (state as WeatherUiState.Stale).snapshot.locationName)
        assertEquals(1234L, state.fetchedAt)
    }

    @Test
    fun `refresh uses NWS api when provider is nws and location is in the US`() = runBlocking {
        val prefs = AppPreferences(FakeDataStore())
        prefs.setWeatherProvider("nws")
        val openMeteo = FakeWeatherApi("open-meteo")
        val nws = FakeWeatherApi("nws")
        val repo = WeatherRepository(openMeteo, nws, FakeAirQualityApi(), prefs)

        repo.refresh("loc-ny", 40.7128, -74.0060, "New York", Units.METRIC) // US coordinates

        assertEquals(0, openMeteo.callCount)
        assertEquals(1, nws.callCount)
    }

    @Test
    fun `refresh falls back to open-meteo for this call WITHOUT resetting the stored preference`() = runBlocking {
        // Behavior change from the single-location version: with multiple saved locations
        // possibly spanning US and non-US, a non-US page's refresh must not silently disable
        // NWS for the user's OTHER, US-based pages by resetting a single global preference.
        val prefs = AppPreferences(FakeDataStore())
        prefs.setWeatherProvider("nws")
        val openMeteo = FakeWeatherApi("open-meteo")
        val nws = FakeWeatherApi("nws")
        val repo = WeatherRepository(openMeteo, nws, FakeAirQualityApi(), prefs)

        repo.refresh("loc-london", 51.5072, -0.1276, "London", Units.METRIC) // non-US coordinates

        assertEquals(1, openMeteo.callCount)
        assertEquals(0, nws.callCount)
        assertEquals("nws", prefs.weatherProvider.firstOrNull()) // unchanged
    }
}
