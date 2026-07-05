package com.fossisawesome.ventus.data

import androidx.annotation.DrawableRes
import com.fossisawesome.ventus.R

enum class Units { METRIC, IMPERIAL }

// Countries that use imperial units for everyday weather reporting.
private val IMPERIAL_COUNTRY_CODES = setOf("US", "LR", "MM")

fun resolveUnits(mode: String, countryCode: String): Units = when (mode) {
    "metric" -> Units.METRIC
    "imperial" -> Units.IMPERIAL
    else -> if (countryCode.uppercase() in IMPERIAL_COUNTRY_CODES) Units.IMPERIAL else Units.METRIC
}

fun celsiusToFahrenheit(c: Double): Double = c * 9.0 / 5.0 + 32.0
fun fahrenheitToCelsius(f: Double): Double = (f - 32.0) * 5.0 / 9.0
fun kmhToMph(kmh: Double): Double = kmh / 1.609344
fun mmToInches(mm: Double): Double = mm / 25.4

data class WeatherCodeInfo(val description: String, @DrawableRes val icon: Int)

// WMO weather interpretation codes, as used by Open-Meteo.
private val WEATHER_CODES: Map<Int, WeatherCodeInfo> = mapOf(
    0 to WeatherCodeInfo("Clear sky", R.drawable.ic_weather_sun),
    1 to WeatherCodeInfo("Mainly clear", R.drawable.ic_weather_cloud_sun),
    2 to WeatherCodeInfo("Partly cloudy", R.drawable.ic_weather_cloud_sun),
    3 to WeatherCodeInfo("Overcast", R.drawable.ic_weather_cloud),
    45 to WeatherCodeInfo("Fog", R.drawable.ic_weather_cloud_fog),
    48 to WeatherCodeInfo("Depositing rime fog", R.drawable.ic_weather_cloud_fog),
    51 to WeatherCodeInfo("Light drizzle", R.drawable.ic_weather_cloud_rain),
    53 to WeatherCodeInfo("Moderate drizzle", R.drawable.ic_weather_cloud_rain),
    55 to WeatherCodeInfo("Dense drizzle", R.drawable.ic_weather_cloud_rain),
    56 to WeatherCodeInfo("Light freezing drizzle", R.drawable.ic_weather_cloud_rain),
    57 to WeatherCodeInfo("Dense freezing drizzle", R.drawable.ic_weather_cloud_rain),
    61 to WeatherCodeInfo("Slight rain", R.drawable.ic_weather_cloud_rain),
    63 to WeatherCodeInfo("Moderate rain", R.drawable.ic_weather_cloud_rain),
    65 to WeatherCodeInfo("Heavy rain", R.drawable.ic_weather_cloud_lightning),
    66 to WeatherCodeInfo("Light freezing rain", R.drawable.ic_weather_cloud_rain),
    67 to WeatherCodeInfo("Heavy freezing rain", R.drawable.ic_weather_cloud_rain),
    71 to WeatherCodeInfo("Slight snow fall", R.drawable.ic_weather_cloud_snow),
    73 to WeatherCodeInfo("Moderate snow fall", R.drawable.ic_weather_cloud_snow),
    75 to WeatherCodeInfo("Heavy snow fall", R.drawable.ic_weather_snowflake),
    77 to WeatherCodeInfo("Snow grains", R.drawable.ic_weather_snowflake),
    80 to WeatherCodeInfo("Slight rain showers", R.drawable.ic_weather_cloud_rain),
    81 to WeatherCodeInfo("Moderate rain showers", R.drawable.ic_weather_cloud_rain),
    82 to WeatherCodeInfo("Violent rain showers", R.drawable.ic_weather_cloud_lightning),
    85 to WeatherCodeInfo("Slight snow showers", R.drawable.ic_weather_cloud_snow),
    86 to WeatherCodeInfo("Heavy snow showers", R.drawable.ic_weather_snowflake),
    95 to WeatherCodeInfo("Thunderstorm", R.drawable.ic_weather_cloud_lightning),
    96 to WeatherCodeInfo("Thunderstorm with slight hail", R.drawable.ic_weather_cloud_lightning),
    99 to WeatherCodeInfo("Thunderstorm with heavy hail", R.drawable.ic_weather_cloud_lightning),
)

fun weatherCodeInfo(code: Int): WeatherCodeInfo =
    WEATHER_CODES[code] ?: WeatherCodeInfo("Unknown", R.drawable.ic_weather_question)
