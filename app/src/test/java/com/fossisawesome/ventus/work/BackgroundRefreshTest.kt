package com.fossisawesome.ventus.work

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.fossisawesome.ventus.data.Units
import com.fossisawesome.ventus.data.api.AirQualityApi
import com.fossisawesome.ventus.data.api.WeatherApi
import com.fossisawesome.ventus.data.location.LocationResult
import com.fossisawesome.ventus.data.location.LocationSource
import com.fossisawesome.ventus.data.model.*
import com.fossisawesome.ventus.data.repository.LocationRepository
import com.fossisawesome.ventus.data.repository.WeatherRepository
import com.fossisawesome.ventus.data.storage.AppPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeDataStore : DataStore<Preferences> {
    private val flow = MutableStateFlow<Preferences>(emptyPreferences())
    override val data = flow
    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
        val updated = transform(flow.value)
        flow.value = updated
        return updated
    }
}

private fun sampleSnapshot(name: String) = WeatherSnapshot(
    locationName = name,
    units = Units.METRIC,
    currentTempC = 20.0,
    currentApparentTempC = 20.0,
    currentHumidity = 50,
    currentWindKmh = 10.0,
    currentWeatherCode = 1,
    hourly = listOf(HourlyPoint(epochSeconds = 0L, tempC = 20.0, precipitationProbability = 0, weatherCode = 1)),
    daily = listOf(DailyPoint(epochSeconds = 0L, tempMaxC = 22.0, tempMinC = 18.0, weatherCode = 1)),
)

private class FakeWeatherApi(private val shouldFail: Boolean = false) : WeatherApi {
    val requestedNames = mutableListOf<String>()
    override suspend fun fetchForecast(locationName: String, units: Units, lat: Double, lon: Double, aqi: Int?): WeatherSnapshot {
        requestedNames.add(locationName)
        if (shouldFail) error("network down")
        return sampleSnapshot(locationName)
    }
}

private class FakeAirQualityApi : AirQualityApi {
    override suspend fun fetchAqi(lat: Double, lon: Double): Int? = null
}

private class FakeLocationSource(private val result: LocationResult) : LocationSource {
    override suspend fun getCurrentLocation(): LocationResult = result
}

class BackgroundRefreshTest {

    @Test
    fun `refreshAllLocations refreshes every saved location, not just the active one`() = runBlocking {
        val prefs = AppPreferences(FakeDataStore())
        val weatherApi = FakeWeatherApi()
        val weatherRepository = WeatherRepository(weatherApi, weatherApi, FakeAirQualityApi(), prefs)
        val locationRepository = LocationRepository(prefs)
        locationRepository.addLocation(Location("geo:1", 1.0, 1.0, "Paris", "France"))
        locationRepository.addLocation(Location("geo:2", 2.0, 2.0, "London", "UK"))
        locationRepository.setActiveLocationId("geo:1") // active is Paris; London must still refresh

        refreshAllLocations(weatherRepository, locationRepository, FakeLocationSource(LocationResult.Unavailable), prefs, "US")

        assertEquals(listOf("Paris", "London"), weatherApi.requestedNames)
        assertTrue(weatherRepository.loadCached("geo:1") is WeatherUiState.Stale)
        assertTrue(weatherRepository.loadCached("geo:2") is WeatherUiState.Stale)
    }

    @Test
    fun `refreshAllLocations re-resolves GPS coords for the current-location entry`() = runBlocking {
        val prefs = AppPreferences(FakeDataStore())
        val weatherApi = FakeWeatherApi()
        val weatherRepository = WeatherRepository(weatherApi, weatherApi, FakeAirQualityApi(), prefs)
        val locationRepository = LocationRepository(prefs)
        locationRepository.addLocation(
            Location(AppPreferences.CURRENT_LOCATION_ID, 1.0, 1.0, "Current location", null, isCurrentLocation = true),
        )

        refreshAllLocations(
            weatherRepository, locationRepository,
            FakeLocationSource(LocationResult.Success(9.0, 9.0)), prefs, "US",
        )

        val updated = locationRepository.locationsFlow.first().first()
        assertEquals(9.0, updated.lat, 0.0001)
        assertEquals(9.0, updated.lon, 0.0001)
    }

    @Test
    fun `refreshAllLocations continues past one location's failure`() = runBlocking {
        val prefs = AppPreferences(FakeDataStore())
        val weatherApi = FakeWeatherApi(shouldFail = true)
        val weatherRepository = WeatherRepository(weatherApi, weatherApi, FakeAirQualityApi(), prefs)
        val locationRepository = LocationRepository(prefs)
        locationRepository.addLocation(Location("geo:1", 1.0, 1.0, "Paris", "France"))
        locationRepository.addLocation(Location("geo:2", 2.0, 2.0, "London", "UK"))

        refreshAllLocations(weatherRepository, locationRepository, FakeLocationSource(LocationResult.Unavailable), prefs, "US")

        assertEquals(listOf("Paris", "London"), weatherApi.requestedNames) // both attempted despite both failing
    }

    @Test
    fun `refreshAllLocations does nothing when there are no saved locations`() = runBlocking {
        val prefs = AppPreferences(FakeDataStore())
        val weatherApi = FakeWeatherApi()
        val weatherRepository = WeatherRepository(weatherApi, weatherApi, FakeAirQualityApi(), prefs)
        val locationRepository = LocationRepository(prefs)

        refreshAllLocations(weatherRepository, locationRepository, FakeLocationSource(LocationResult.Unavailable), prefs, "US")

        assertTrue(weatherApi.requestedNames.isEmpty())
    }
}
