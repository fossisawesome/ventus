package com.fossisawesome.ventus.viewmodel

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.fossisawesome.ventus.data.Units
import com.fossisawesome.ventus.data.api.GeocodingApi
import com.fossisawesome.ventus.data.api.WeatherApi
import com.fossisawesome.ventus.data.location.LocationResult
import com.fossisawesome.ventus.data.location.LocationSource
import com.fossisawesome.ventus.data.model.*
import com.fossisawesome.ventus.data.repository.WeatherRepository
import com.fossisawesome.ventus.data.storage.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
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

private val sampleResponse = OpenMeteoForecastResponse(
    latitude = 40.7128, longitude = -74.0060,
    current = CurrentBlock("2026-07-05T14:00", 22.5, 60, 23.0, 2, 12.3),
    hourly = HourlyBlock(listOf("2026-07-05T14:00"), listOf(22.5), listOf(10), listOf(2)),
    daily = DailyBlock(listOf("2026-07-05"), listOf(2), listOf(25.0), listOf(15.0)),
)

private class FakeWeatherApi : WeatherApi {
    override suspend fun fetchForecast(lat: Double, lon: Double, units: Units) = sampleResponse
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

    @Test
    fun `loadInitial with granted GPS fetches weather and reaches Success`() = runTest {
        val prefs = AppPreferences(FakeDataStore())
        val repo = WeatherRepository(FakeWeatherApi(), prefs)
        val vm = WeatherViewModel(
            repository = repo,
            locationSource = FakeLocationSource(LocationResult.Success(40.7128, -74.0060)),
            geocodingApi = FakeGeocodingApi(),
            prefs = prefs,
            countryCode = "US",
        )

        vm.loadInitial()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.state.value is WeatherUiState.Success)
    }

    @Test
    fun `loadInitial with denied permission and no cache reaches NeedsLocation`() = runTest {
        val prefs = AppPreferences(FakeDataStore())
        val repo = WeatherRepository(FakeWeatherApi(), prefs)
        val vm = WeatherViewModel(
            repository = repo,
            locationSource = FakeLocationSource(LocationResult.PermissionDenied),
            geocodingApi = FakeGeocodingApi(),
            prefs = prefs,
            countryCode = "US",
        )

        vm.loadInitial()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(WeatherUiState.NeedsLocation, vm.state.value)
    }

    @Test
    fun `selectLocation saves location and fetches weather`() = runTest {
        val prefs = AppPreferences(FakeDataStore())
        val repo = WeatherRepository(FakeWeatherApi(), prefs)
        val vm = WeatherViewModel(
            repository = repo,
            locationSource = FakeLocationSource(LocationResult.PermissionDenied),
            geocodingApi = FakeGeocodingApi(),
            prefs = prefs,
            countryCode = "US",
        )

        vm.selectLocation(GeocodingResult(1, "Paris", 48.8566, 2.3522, "France", null))
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.state.value is WeatherUiState.Success)
        assertEquals("Paris, France", prefs.locationName.first())
    }

    @Test
    fun `search populates searchResults from geocoding api`() = runTest {
        val prefs = AppPreferences(FakeDataStore())
        val repo = WeatherRepository(FakeWeatherApi(), prefs)
        val fakeResults = listOf(GeocodingResult(1, "London", 51.5, -0.12, "UK", null))
        val vm = WeatherViewModel(
            repository = repo,
            locationSource = FakeLocationSource(LocationResult.Unavailable),
            geocodingApi = FakeGeocodingApi(fakeResults),
            prefs = prefs,
            countryCode = "US",
        )

        vm.search("Lon")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(fakeResults, vm.searchResults.value)
    }
}
