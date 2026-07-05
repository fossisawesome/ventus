package com.fossisawesome.ventus.data

import com.fossisawesome.ventus.R
import org.junit.Assert.assertEquals
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
}
