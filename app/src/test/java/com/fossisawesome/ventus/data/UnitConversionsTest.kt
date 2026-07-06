package com.fossisawesome.ventus.data

import com.fossisawesome.ventus.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UnitConversionsTest {

    @Test
    fun `celsius to fahrenheit`() {
        assertEquals(32.0, celsiusToFahrenheit(0.0), 0.001)
        assertEquals(212.0, celsiusToFahrenheit(100.0), 0.001)
    }

    @Test
    fun `fahrenheit to celsius`() {
        assertEquals(0.0, fahrenheitToCelsius(32.0), 0.001)
        assertEquals(100.0, fahrenheitToCelsius(212.0), 0.001)
    }

    @Test
    fun `kmh to mph`() {
        assertEquals(62.1371, kmhToMph(100.0), 0.001)
    }

    @Test
    fun `mm to inches`() {
        assertEquals(1.0, mmToInches(25.4), 0.001)
    }

    @Test
    fun `tempValue rounds celsius directly when not imperial`() {
        assertEquals(23, tempValue(22.5, isImperial = false))
    }

    @Test
    fun `tempValue converts to fahrenheit and rounds when imperial`() {
        assertEquals(73, tempValue(22.6, isImperial = true)) // 22.6C = 72.68F, rounds to 73
    }

    @Test
    fun `resolves imperial for US`() {
        assertEquals(Units.IMPERIAL, resolveUnits("auto", "US"))
    }

    @Test
    fun `resolves metric for non-US locale`() {
        assertEquals(Units.METRIC, resolveUnits("auto", "DE"))
    }

    @Test
    fun `explicit mode overrides locale`() {
        assertEquals(Units.METRIC, resolveUnits("metric", "US"))
        assertEquals(Units.IMPERIAL, resolveUnits("imperial", "DE"))
    }

    @Test
    fun `known weather codes map to description and icon`() {
        assertEquals(WeatherCodeInfo("Clear sky", R.drawable.ic_weather_sun), weatherCodeInfo(0))
        assertEquals(WeatherCodeInfo("Thunderstorm", R.drawable.ic_weather_cloud_lightning), weatherCodeInfo(95))
    }

    @Test
    fun `partly cloudy maps to cloud sun icon`() {
        assertEquals(R.drawable.ic_weather_cloud_sun, weatherCodeInfo(2).icon)
    }

    @Test
    fun `unknown weather code falls back to a generic description`() {
        val info = weatherCodeInfo(999)
        assertEquals("Unknown", info.description)
        assertEquals(R.drawable.ic_weather_question, info.icon)
    }

    @Test
    fun `mph to kmh`() {
        assertEquals(160.9344, mphToKmh(100.0), 0.001)
    }

    @Test
    fun `inches to mm`() {
        assertEquals(25.4, inchesToMm(1.0), 0.001)
    }

    @Test
    fun `us location detects continental US`() {
        assertTrue(isUsLocation(40.7128, -74.0060)) // New York
    }

    @Test
    fun `us location detects alaska and hawaii`() {
        assertTrue(isUsLocation(61.2181, -149.9003)) // Anchorage
        assertTrue(isUsLocation(21.3069, -157.8583)) // Honolulu
    }

    @Test
    fun `us location rejects non-us coordinates`() {
        assertFalse(isUsLocation(51.5072, -0.1276)) // London
        assertFalse(isUsLocation(35.6762, 139.6503)) // Tokyo
    }

    @Test
    fun `nws icon keyword maps to closest wmo code`() {
        assertEquals(0, nwsIconToWeatherCode("https://api.weather.gov/icons/land/day/skc?size=medium"))
        assertEquals(1, nwsIconToWeatherCode("https://api.weather.gov/icons/land/day/few?size=medium"))
        assertEquals(2, nwsIconToWeatherCode("https://api.weather.gov/icons/land/day/sct?size=medium"))
        assertEquals(3, nwsIconToWeatherCode("https://api.weather.gov/icons/land/day/bkn?size=medium"))
        assertEquals(61, nwsIconToWeatherCode("https://api.weather.gov/icons/land/day/rain,40?size=medium"))
        assertEquals(80, nwsIconToWeatherCode("https://api.weather.gov/icons/land/day/rain_showers,40?size=medium"))
        assertEquals(95, nwsIconToWeatherCode("https://api.weather.gov/icons/land/day/tsra,40?size=medium"))
        assertEquals(71, nwsIconToWeatherCode("https://api.weather.gov/icons/land/day/snow,40?size=medium"))
        assertEquals(45, nwsIconToWeatherCode("https://api.weather.gov/icons/land/day/fog?size=medium"))
        assertEquals(3, nwsIconToWeatherCode(null))
    }
}
