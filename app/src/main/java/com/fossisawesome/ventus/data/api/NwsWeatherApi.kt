package com.fossisawesome.ventus.data.api

import com.fossisawesome.ventus.data.Units
import com.fossisawesome.ventus.data.fahrenheitToCelsius
import com.fossisawesome.ventus.data.mphToKmh
import com.fossisawesome.ventus.data.model.*
import com.fossisawesome.ventus.data.nwsIconToWeatherCode
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

class NwsWeatherApi(
    private val client: OkHttpClient = OkHttpClient(),
) : WeatherApi {

    private val gson = Gson()

    // api.weather.gov requires a User-Agent identifying the calling application; requests
    // without one are rejected.
    private fun request(url: String) = Request.Builder()
        .url(url)
        .header("User-Agent", "Ventus Weather App (https://github.com/fossisawesome/ventus)")
        .build()

    private fun <T> fetchJson(url: String, type: Class<T>): T {
        val response = client.newCall(request(url)).execute()
        if (!response.isSuccessful) error("NWS request failed with HTTP ${response.code} for $url")
        val body = response.body?.string() ?: error("Empty response from $url")
        return gson.fromJson(body, type)
    }

    override suspend fun fetchForecast(locationName: String, units: Units, lat: Double, lon: Double, aqi: Int?): WeatherSnapshot =
        withContext(Dispatchers.IO) {
            val points = fetchJson("https://api.weather.gov/points/$lat,$lon", NwsPointsResponse::class.java)
            val forecastUrl = points.properties?.forecast ?: error("NWS points response missing forecast URL")
            val forecastHourlyUrl = points.properties.forecastHourly ?: error("NWS points response missing forecastHourly URL")

            val hourly = fetchJson(forecastHourlyUrl, NwsForecastResponse::class.java)
                .properties?.periods ?: error("NWS hourly forecast missing periods")
            val daily = fetchJson(forecastUrl, NwsForecastResponse::class.java)
                .properties?.periods ?: error("NWS forecast missing periods")

            mapNwsPeriodsToSnapshot(locationName, units, hourly, daily, aqi)
        }
}

// Kept internal (not private) so NwsWeatherApiMappingTest can exercise the mapping without
// mocking HTTP, matching the pattern used by mapForecastResponse() for Open-Meteo.
internal fun mapNwsPeriodsToSnapshot(
    locationName: String,
    units: Units,
    hourlyPeriods: List<NwsPeriod>,
    forecastPeriods: List<NwsPeriod>,
    aqi: Int?,
): WeatherSnapshot {
    val nowPeriod = hourlyPeriods.firstOrNull() ?: error("NWS hourly forecast is empty")

    val hourlyPoints = hourlyPeriods.take(24).map { period ->
        HourlyPoint(
            epochSeconds = OffsetDateTime.parse(period.startTime, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toEpochSecond(),
            tempC = fahrenheitToCelsius(period.temperature),
            precipitationProbability = period.probabilityOfPrecipitation?.value ?: 0,
            weatherCode = nwsIconToWeatherCode(period.icon),
        )
    }

    // NWS forecast periods alternate day/night (e.g. "Today"/"Tonight"/"Wednesday"/"Wednesday
    // Night"), each ~12h. Group by calendar date (the date portion of startTime) so each entry
    // becomes one DailyPoint, using the daytime period's high and the following night's low.
    val dailyPoints = forecastPeriods
        .groupBy { it.startTime.substring(0, 10) }
        .entries.sortedBy { it.key }
        .map { (date, periodsForDate) ->
            val dayPeriod = periodsForDate.firstOrNull { it.isDaytime } ?: periodsForDate.first()
            val nightPeriod = periodsForDate.firstOrNull { !it.isDaytime } ?: periodsForDate.last()
            DailyPoint(
                epochSeconds = OffsetDateTime.parse("${date}T00:00:00${dayPeriod.startTime.substring(19)}", DateTimeFormatter.ISO_OFFSET_DATE_TIME).toEpochSecond(),
                tempMaxC = fahrenheitToCelsius(dayPeriod.temperature),
                tempMinC = fahrenheitToCelsius(nightPeriod.temperature),
                weatherCode = nwsIconToWeatherCode(dayPeriod.icon),
                precipitationProbability = dayPeriod.probabilityOfPrecipitation?.value ?: 0,
            )
        }

    // NWS windSpeed is a free-text field like "10 mph" or "10 to 15 mph" — take the first number.
    val windMph = Regex("""\d+""").find(nowPeriod.windSpeed ?: "0")?.value?.toDoubleOrNull() ?: 0.0

    return WeatherSnapshot(
        locationName = locationName,
        units = units,
        currentTempC = fahrenheitToCelsius(nowPeriod.temperature),
        currentApparentTempC = null, // NWS's period payloads have no apparent-temperature field
        currentHumidity = null, // NWS's period payloads have no humidity field
        currentWindKmh = mphToKmh(windMph),
        currentWeatherCode = nwsIconToWeatherCode(nowPeriod.icon),
        hourly = hourlyPoints,
        daily = dailyPoints,
        sunriseEpochSeconds = null, // not available from NWS forecast endpoints
        sunsetEpochSeconds = null,
        uvIndex = null, // not available from NWS forecast endpoints
        aqi = aqi,
    )
}
