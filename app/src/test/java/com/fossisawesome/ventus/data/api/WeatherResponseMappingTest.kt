package com.fossisawesome.ventus.data.api

import com.fossisawesome.ventus.data.Units
import com.fossisawesome.ventus.data.model.CurrentBlock
import com.fossisawesome.ventus.data.model.DailyBlock
import com.fossisawesome.ventus.data.model.HourlyBlock
import com.fossisawesome.ventus.data.model.OpenMeteoForecastResponse
import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherResponseMappingTest {

    private val sampleResponse = OpenMeteoForecastResponse(
        latitude = 40.7128,
        longitude = -74.0060,
        current = CurrentBlock(
            time = "2026-07-05T14:00",
            temperature2m = 22.5,
            relativeHumidity2m = 60,
            apparentTemperature = 23.0,
            weatherCode = 2,
            windSpeed10m = 12.3,
        ),
        hourly = HourlyBlock(
            time = listOf("2026-07-05T14:00", "2026-07-05T15:00"),
            temperature2m = listOf(22.5, 23.0),
            precipitationProbability = listOf(10, 15),
            weatherCode = listOf(2, 2),
        ),
        daily = DailyBlock(
            time = listOf("2026-07-05", "2026-07-06"),
            weatherCode = listOf(2, 61),
            temperature2mMax = listOf(25.0, 20.0),
            temperature2mMin = listOf(15.0, 14.0),
        ),
    )

    @Test
    fun `maps current block into snapshot`() {
        val snapshot = mapForecastResponse("New York", Units.METRIC, sampleResponse)
        assertEquals("New York", snapshot.locationName)
        assertEquals(Units.METRIC, snapshot.units)
        assertEquals(22.5, snapshot.currentTempC, 0.001)
        assertEquals(23.0, snapshot.currentApparentTempC, 0.001)
        assertEquals(60, snapshot.currentHumidity)
        assertEquals(12.3, snapshot.currentWindKmh, 0.001)
        assertEquals(2, snapshot.currentWeatherCode)
    }

    @Test
    fun `maps hourly block preserving order`() {
        val snapshot = mapForecastResponse("New York", Units.METRIC, sampleResponse)
        assertEquals(2, snapshot.hourly.size)
        assertEquals(22.5, snapshot.hourly[0].tempC, 0.001)
        assertEquals(23.0, snapshot.hourly[1].tempC, 0.001)
        assertEquals(10, snapshot.hourly[0].precipitationProbability)
    }

    @Test
    fun `maps daily block preserving order`() {
        val snapshot = mapForecastResponse("New York", Units.METRIC, sampleResponse)
        assertEquals(2, snapshot.daily.size)
        assertEquals(25.0, snapshot.daily[0].tempMaxC, 0.001)
        assertEquals(15.0, snapshot.daily[0].tempMinC, 0.001)
        assertEquals(61, snapshot.daily[1].weatherCode)
    }

    @Test(expected = IllegalStateException::class)
    fun `throws when current block is missing`() {
        mapForecastResponse("New York", Units.METRIC, sampleResponse.copy(current = null))
    }
}
