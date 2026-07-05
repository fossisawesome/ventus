package com.fossisawesome.ventus.data.api

import com.fossisawesome.ventus.data.Units
import com.fossisawesome.ventus.data.model.*
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

interface WeatherApi {
    suspend fun fetchForecast(lat: Double, lon: Double): OpenMeteoForecastResponse
}

class OpenMeteoWeatherApi(
    private val client: OkHttpClient = OkHttpClient(),
) : WeatherApi {

    private val gson = Gson()

    // Always requested in metric — WeatherSnapshot stores SI values regardless of the user's
    // display unit preference; conversion for display happens in the UI layer (see
    // UnitConversions.kt). Requesting the API's own unit conversion here would double-convert
    // once the UI applies celsiusToFahrenheit()/kmhToMph() on top.
    override suspend fun fetchForecast(lat: Double, lon: Double): OpenMeteoForecastResponse =
        withContext(Dispatchers.IO) {
            val url = "https://api.open-meteo.com/v1/forecast".toHttpUrl()
                .newBuilder()
                .addQueryParameter("latitude", lat.toString())
                .addQueryParameter("longitude", lon.toString())
                .addQueryParameter("current", "temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m")
                .addQueryParameter("hourly", "temperature_2m,precipitation_probability,weather_code")
                .addQueryParameter("daily", "weather_code,temperature_2m_max,temperature_2m_min")
                .addQueryParameter("temperature_unit", "celsius")
                .addQueryParameter("wind_speed_unit", "kmh")
                .addQueryParameter("precipitation_unit", "mm")
                .addQueryParameter("timezone", "auto")
                .addQueryParameter("forecast_days", "7")
                .build()

            val response = client.newCall(Request.Builder().url(url).build()).execute()
            if (!response.isSuccessful) error("Weather request failed with HTTP ${response.code}")
            val body = response.body?.string() ?: error("Empty response from forecast endpoint")
            gson.fromJson(body, OpenMeteoForecastResponse::class.java)
        }
}

// Converts an Open-Meteo ISO-local-time string (e.g. "2026-07-05T14:00") to epoch seconds,
// interpreting it as UTC (the API's "timezone=auto" values are local to the queried location,
// but for relative ordering/display within a single location this is sufficient).
internal fun isoLocalTimeToEpochSeconds(iso: String): Long =
    LocalDateTime.parse(iso, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"))
        .toEpochSecond(ZoneOffset.UTC)

fun mapForecastResponse(locationName: String, units: Units, response: OpenMeteoForecastResponse): WeatherSnapshot {
    val current = response.current ?: error("Forecast response missing current block")
    val hourly = response.hourly
    val daily = response.daily

    val hourlyPoints = if (hourly != null) {
        hourly.time.indices.map { i ->
            HourlyPoint(
                epochSeconds = isoLocalTimeToEpochSeconds(hourly.time[i]),
                tempC = hourly.temperature2m[i],
                precipitationProbability = hourly.precipitationProbability[i],
                weatherCode = hourly.weatherCode[i],
            )
        }
    } else emptyList()

    val dailyPoints = if (daily != null) {
        daily.time.indices.map { i ->
            DailyPoint(
                epochSeconds = isoLocalTimeToEpochSeconds(daily.time[i] + "T00:00"),
                tempMaxC = daily.temperature2mMax[i],
                tempMinC = daily.temperature2mMin[i],
                weatherCode = daily.weatherCode[i],
            )
        }
    } else emptyList()

    return WeatherSnapshot(
        locationName = locationName,
        units = units,
        currentTempC = current.temperature2m,
        currentApparentTempC = current.apparentTemperature,
        currentHumidity = current.relativeHumidity2m,
        currentWindKmh = current.windSpeed10m,
        currentWeatherCode = current.weatherCode,
        hourly = hourlyPoints,
        daily = dailyPoints,
    )
}
