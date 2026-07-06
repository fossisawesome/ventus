package com.fossisawesome.ventus.viewmodel

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.fossisawesome.ventus.data.Units
import com.fossisawesome.ventus.data.api.AirQualityApi
import com.fossisawesome.ventus.data.api.GeocodingApi
import com.fossisawesome.ventus.data.api.WeatherApi
import com.fossisawesome.ventus.data.location.LocationResult
import com.fossisawesome.ventus.data.location.LocationSource
import com.fossisawesome.ventus.data.model.*
import com.fossisawesome.ventus.data.repository.LocationRepository
import com.fossisawesome.ventus.data.repository.WeatherRepository
import com.fossisawesome.ventus.data.storage.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
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
    currentTempC = 22.5,
    currentApparentTempC = 23.0,
    currentHumidity = 60,
    currentWindKmh = 12.3,
    currentWeatherCode = 2,
    hourly = listOf(HourlyPoint(epochSeconds = 0L, tempC = 22.5, precipitationProbability = 10, weatherCode = 2)),
    daily = listOf(DailyPoint(epochSeconds = 0L, tempMaxC = 25.0, tempMinC = 15.0, weatherCode = 2)),
)

private class FakeWeatherApi : WeatherApi {
    override suspend fun fetchForecast(locationName: String, units: Units, lat: Double, lon: Double, aqi: Int?) =
        sampleSnapshot(locationName)
}

private class FakeAirQualityApi : AirQualityApi {
    override suspend fun fetchAqi(lat: Double, lon: Double): Int? = null
}

private class FakeGeocodingApi(private val results: List<GeocodingResult> = emptyList()) : GeocodingApi {
    override suspend fun search(query: String): List<GeocodingResult> = results
}

private class FakeLocationSource(private val result: LocationResult) : LocationSource {
    override suspend fun getCurrentLocation(): LocationResult = result
}

@OptIn(ExperimentalCoroutinesApi::class)
class WeatherViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() { Dispatchers.setMain(testDispatcher) }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    private fun buildVm(locationSource: LocationSource, geocodingApi: GeocodingApi = FakeGeocodingApi()): Pair<WeatherViewModel, AppPreferences> {
        val prefs = AppPreferences(FakeDataStore())
        val weatherRepo = WeatherRepository(FakeWeatherApi(), FakeWeatherApi(), FakeAirQualityApi(), prefs)
        val locationRepo = LocationRepository(prefs)
        val vm = WeatherViewModel(weatherRepo, locationRepo, locationSource, geocodingApi, prefs, "US")
        return vm to prefs
    }

    @Test
    fun `loadInitial with granted GPS and no saved locations adds a current-location entry and reaches Success`() = runTest {
        val (vm, _) = buildVm(FakeLocationSource(LocationResult.Success(40.7128, -74.0060)))

        vm.loadInitial()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, vm.locations.value.size)
        assertTrue(vm.locations.value.first().isCurrentLocation)
        assertTrue(vm.weatherStates.value.values.first() is WeatherUiState.Success)
    }

    @Test
    fun `loadInitial with denied permission and no saved locations ends with an empty location list`() = runTest {
        val (vm, _) = buildVm(FakeLocationSource(LocationResult.PermissionDenied))

        vm.loadInitial()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.locations.value.isEmpty())
    }

    @Test
    fun `addLocationFromSearch saves the location, sets it active, and fetches its weather`() = runTest {
        val (vm, _) = buildVm(FakeLocationSource(LocationResult.PermissionDenied))

        vm.addLocationFromSearch(GeocodingResult(1, "Paris", 48.8566, 2.3522, "France", null))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, vm.locations.value.size)
        assertEquals("Paris, France", vm.locations.value.first().name)
        assertEquals("geo:1", vm.activeLocationId.value)
        assertTrue(vm.weatherStates.value["geo:1"] is WeatherUiState.Success)
    }

    @Test
    fun `adding a second location keeps the first one's weather state untouched`() = runTest {
        val (vm, _) = buildVm(FakeLocationSource(LocationResult.PermissionDenied))
        vm.addLocationFromSearch(GeocodingResult(1, "Paris", 48.8566, 2.3522, "France", null))
        testDispatcher.scheduler.advanceUntilIdle()

        vm.addLocationFromSearch(GeocodingResult(2, "London", 51.5, -0.12, "UK", null))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, vm.locations.value.size)
        assertTrue(vm.weatherStates.value["geo:1"] is WeatherUiState.Success)
        assertTrue(vm.weatherStates.value["geo:2"] is WeatherUiState.Success)
    }

    @Test
    fun `addLocationFromSearch reports the limit message once MAX_SAVED_LOCATIONS is reached`() = runTest {
        val (vm, _) = buildVm(FakeLocationSource(LocationResult.PermissionDenied))
        repeat(AppPreferences.MAX_SAVED_LOCATIONS) { i ->
            vm.addLocationFromSearch(GeocodingResult(i.toLong(), "City $i", i.toDouble(), i.toDouble(), null, null))
        }
        testDispatcher.scheduler.advanceUntilIdle()

        vm.addLocationFromSearch(GeocodingResult(999, "Overflow", 99.0, 99.0, null, null))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(AppPreferences.MAX_SAVED_LOCATIONS, vm.locations.value.size)
        assertTrue(vm.locationLimitMessage.value != null)
    }

    @Test
    fun `removeLocation drops it from both locations and weatherStates`() = runTest {
        val (vm, _) = buildVm(FakeLocationSource(LocationResult.PermissionDenied))
        vm.addLocationFromSearch(GeocodingResult(1, "Paris", 48.8566, 2.3522, "France", null))
        testDispatcher.scheduler.advanceUntilIdle()

        vm.removeLocation("geo:1")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.locations.value.isEmpty())
        assertTrue(vm.weatherStates.value["geo:1"] == null)
    }

    @Test
    fun `selectLocation jumps the active id to an already-saved location without re-adding it`() = runTest {
        val (vm, _) = buildVm(FakeLocationSource(LocationResult.PermissionDenied))
        vm.addLocationFromSearch(GeocodingResult(1, "Paris", 48.8566, 2.3522, "France", null))
        vm.addLocationFromSearch(GeocodingResult(2, "London", 51.5, -0.12, "UK", null))
        testDispatcher.scheduler.advanceUntilIdle()

        vm.selectLocation("geo:1")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("geo:1", vm.activeLocationId.value)
        assertEquals(2, vm.locations.value.size) // unchanged, no duplicate
    }

    @Test
    fun `search populates searchResults from the geocoding api`() = runTest {
        val fakeResults = listOf(GeocodingResult(1, "London", 51.5, -0.12, "UK", null))
        val (vm, _) = buildVm(FakeLocationSource(LocationResult.Unavailable), FakeGeocodingApi(fakeResults))

        vm.search("Lon")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(fakeResults, vm.searchResults.value)
    }
}
