package com.fossisawesome.ventus.data

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

data class WeatherCodeInfo(val description: String, val icon: String)

// WMO weather interpretation codes, as used by Open-Meteo.
private val WEATHER_CODES: Map<Int, WeatherCodeInfo> = mapOf(
    0 to WeatherCodeInfo("Clear sky", "☀️"),
    1 to WeatherCodeInfo("Mainly clear", "🌤️"),
    2 to WeatherCodeInfo("Partly cloudy", "⛅"),
    3 to WeatherCodeInfo("Overcast", "☁️"),
    45 to WeatherCodeInfo("Fog", "🌫️"),
    48 to WeatherCodeInfo("Depositing rime fog", "🌫️"),
    51 to WeatherCodeInfo("Light drizzle", "🌦️"),
    53 to WeatherCodeInfo("Moderate drizzle", "🌦️"),
    55 to WeatherCodeInfo("Dense drizzle", "🌧️"),
    56 to WeatherCodeInfo("Light freezing drizzle", "🌧️"),
    57 to WeatherCodeInfo("Dense freezing drizzle", "🌧️"),
    61 to WeatherCodeInfo("Slight rain", "🌧️"),
    63 to WeatherCodeInfo("Moderate rain", "🌧️"),
    65 to WeatherCodeInfo("Heavy rain", "⛈️"),
    66 to WeatherCodeInfo("Light freezing rain", "🌨️"),
    67 to WeatherCodeInfo("Heavy freezing rain", "🌨️"),
    71 to WeatherCodeInfo("Slight snow fall", "🌨️"),
    73 to WeatherCodeInfo("Moderate snow fall", "🌨️"),
    75 to WeatherCodeInfo("Heavy snow fall", "❄️"),
    77 to WeatherCodeInfo("Snow grains", "❄️"),
    80 to WeatherCodeInfo("Slight rain showers", "🌦️"),
    81 to WeatherCodeInfo("Moderate rain showers", "🌧️"),
    82 to WeatherCodeInfo("Violent rain showers", "⛈️"),
    85 to WeatherCodeInfo("Slight snow showers", "🌨️"),
    86 to WeatherCodeInfo("Heavy snow showers", "❄️"),
    95 to WeatherCodeInfo("Thunderstorm", "⛈️"),
    96 to WeatherCodeInfo("Thunderstorm with slight hail", "⛈️"),
    99 to WeatherCodeInfo("Thunderstorm with heavy hail", "⛈️"),
)

fun weatherCodeInfo(code: Int): WeatherCodeInfo =
    WEATHER_CODES[code] ?: WeatherCodeInfo("Unknown", "❓")
