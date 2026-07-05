package com.fossisawesome.ventus.data.api

import com.fossisawesome.ventus.data.Units
import com.fossisawesome.ventus.data.model.NwsPeriod
import com.fossisawesome.ventus.data.model.NwsProbabilityOfPrecipitation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NwsWeatherApiMappingTest {

    private val hourlyPeriods = listOf(
        NwsPeriod(
            number = 1, name = null,
            startTime = "2026-07-05T14:00:00-04:00", endTime = "2026-07-05T15:00:00-04:00",
            isDaytime = true, temperature = 77.0, temperatureUnit = "F",
            windSpeed = "10 mph", shortForecast = "Sunny",
            icon = "https://api.weather.gov/icons/land/day/skc?size=small",
            probabilityOfPrecipitation = NwsProbabilityOfPrecipitation(5),
        ),
        NwsPeriod(
            number = 2, name = null,
            startTime = "2026-07-05T15:00:00-04:00", endTime = "2026-07-05T16:00:00-04:00",
            isDaytime = true, temperature = 78.0, temperatureUnit = "F",
            windSpeed = "12 mph", shortForecast = "Sunny",
            icon = "https://api.weather.gov/icons/land/day/skc?size=small",
            probabilityOfPrecipitation = NwsProbabilityOfPrecipitation(5),
        ),
    )

    private val forecastPeriods = listOf(
        NwsPeriod(
            number = 1, name = "Today",
            startTime = "2026-07-05T14:00:00-04:00", endTime = "2026-07-05T18:00:00-04:00",
            isDaytime = true, temperature = 80.0, temperatureUnit = "F",
            windSpeed = "10 mph", shortForecast = "Sunny",
            icon = "https://api.weather.gov/icons/land/day/skc?size=medium",
            probabilityOfPrecipitation = NwsProbabilityOfPrecipitation(5),
        ),
        NwsPeriod(
            number = 2, name = "Tonight",
            startTime = "2026-07-05T18:00:00-04:00", endTime = "2026-07-06T06:00:00-04:00",
            isDaytime = false, temperature = 60.0, temperatureUnit = "F",
            windSpeed = "5 mph", shortForecast = "Clear",
            icon = "https://api.weather.gov/icons/land/night/skc?size=medium",
            probabilityOfPrecipitation = NwsProbabilityOfPrecipitation(null),
        ),
    )

    @Test
    fun `maps hourly and forecast periods into a WeatherSnapshot`() {
        val snapshot = mapNwsPeriodsToSnapshot("Test City", Units.METRIC, hourlyPeriods, forecastPeriods, aqi = 42)

        assertEquals("Test City", snapshot.locationName)
        assertEquals(25.0, snapshot.currentTempC, 0.01) // 77F -> 25.0C
        assertNull(snapshot.currentApparentTempC)
        assertNull(snapshot.currentHumidity)
        assertEquals(16.09, snapshot.currentWindKmh, 0.1) // 10 mph -> ~16.09 km/h
        assertEquals(0, snapshot.currentWeatherCode) // skc -> clear (0)
        assertEquals(42, snapshot.aqi)
        assertNull(snapshot.uvIndex)
        assertNull(snapshot.sunriseEpochSeconds)

        assertEquals(2, snapshot.hourly.size)
        assertEquals(25.0, snapshot.hourly[0].tempC, 0.01)
        assertEquals(5, snapshot.hourly[0].precipitationProbability)

        assertEquals(1, snapshot.daily.size)
        val day = snapshot.daily[0]
        assertEquals(26.67, day.tempMaxC, 0.1) // 80F -> ~26.67C
        assertEquals(15.56, day.tempMinC, 0.1) // 60F -> ~15.56C
        assertEquals(0, day.weatherCode) // daytime period's icon (skc)
        assertEquals(5, day.precipitationProbability)
    }
}
