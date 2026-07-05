package com.fossisawesome.ventus.data.model

import com.fossisawesome.ventus.data.Units
import com.google.gson.annotations.SerializedName

// ── Open-Meteo forecast response (raw Gson shape) ──────────────────────────

data class OpenMeteoForecastResponse(
    val latitude: Double,
    val longitude: Double,
    val current: CurrentBlock?,
    val hourly: HourlyBlock?,
    val daily: DailyBlock?,
)

data class CurrentBlock(
    val time: String,
    @SerializedName("temperature_2m") val temperature2m: Double,
    @SerializedName("relative_humidity_2m") val relativeHumidity2m: Int,
    @SerializedName("apparent_temperature") val apparentTemperature: Double,
    @SerializedName("weather_code") val weatherCode: Int,
    @SerializedName("wind_speed_10m") val windSpeed10m: Double,
)

data class HourlyBlock(
    val time: List<String>,
    @SerializedName("temperature_2m") val temperature2m: List<Double>,
    @SerializedName("precipitation_probability") val precipitationProbability: List<Int>,
    @SerializedName("weather_code") val weatherCode: List<Int>,
)

data class DailyBlock(
    val time: List<String>,
    @SerializedName("weather_code") val weatherCode: List<Int>,
    @SerializedName("temperature_2m_max") val temperature2mMax: List<Double>,
    @SerializedName("temperature_2m_min") val temperature2mMin: List<Double>,
    val sunrise: List<String>? = null,
    val sunset: List<String>? = null,
    @SerializedName("uv_index_max") val uvIndexMax: List<Double>? = null,
    @SerializedName("precipitation_probability_max") val precipitationProbabilityMax: List<Int>? = null,
)

// ── Open-Meteo air quality response ─────────────────────────────────────────

data class OpenMeteoAirQualityResponse(
    val current: AirQualityCurrentBlock?,
)

data class AirQualityCurrentBlock(
    @SerializedName("us_aqi") val usAqi: Int?,
)

// ── Open-Meteo geocoding response ───────────────────────────────────────────

data class GeocodingSearchResponse(
    val results: List<GeocodingResult>?,
)

data class GeocodingResult(
    val id: Long,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val country: String?,
    val admin1: String?,
)

// ── NWS (api.weather.gov) forecast response (raw Gson shape) ───────────────
// NWS is a 3-call flow: /points/{lat},{lon} returns forecast/forecastHourly URLs, which are then
// fetched separately. Both return the same periods-list shape under `properties`.

data class NwsPointsResponse(val properties: NwsPointsProperties?)

data class NwsPointsProperties(
    val forecast: String?,
    val forecastHourly: String?,
)

data class NwsForecastResponse(val properties: NwsForecastProperties?)

data class NwsForecastProperties(val periods: List<NwsPeriod>?)

data class NwsPeriod(
    val number: Int,
    val name: String?,
    val startTime: String,
    val endTime: String,
    val isDaytime: Boolean,
    val temperature: Double,
    val temperatureUnit: String?,
    val windSpeed: String?,
    val shortForecast: String?,
    val icon: String?,
    val probabilityOfPrecipitation: NwsProbabilityOfPrecipitation? = null,
)

data class NwsProbabilityOfPrecipitation(val value: Int?)

// ── Domain model used by the repository, viewmodel, and UI ─────────────────
// All temperature/wind/precipitation fields are stored in SI units (Celsius, km/h, mm)
// regardless of display `units` — the UI layer converts at render time (see UnitConversions.kt).

data class WeatherSnapshot(
    val locationName: String,
    val units: Units,
    val currentTempC: Double,
    val currentApparentTempC: Double?,
    val currentHumidity: Int?,
    val currentWindKmh: Double,
    val currentWeatherCode: Int,
    val hourly: List<HourlyPoint>,
    val daily: List<DailyPoint>,
    val sunriseEpochSeconds: Long? = null,
    val sunsetEpochSeconds: Long? = null,
    val uvIndex: Double? = null,
    val aqi: Int? = null,
)

data class HourlyPoint(
    val epochSeconds: Long,
    val tempC: Double,
    val precipitationProbability: Int,
    val weatherCode: Int,
)

data class DailyPoint(
    val epochSeconds: Long,
    val tempMaxC: Double,
    val tempMinC: Double,
    val weatherCode: Int,
    val precipitationProbability: Int = 0,
)

sealed interface WeatherUiState {
    data object Loading : WeatherUiState
    data object NeedsLocation : WeatherUiState
    data class Success(val snapshot: WeatherSnapshot) : WeatherUiState
    data class Stale(val snapshot: WeatherSnapshot, val fetchedAt: Long) : WeatherUiState
    data class Error(val message: String) : WeatherUiState
}
