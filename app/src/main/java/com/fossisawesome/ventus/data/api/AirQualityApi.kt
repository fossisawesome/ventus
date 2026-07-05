package com.fossisawesome.ventus.data.api

import com.fossisawesome.ventus.data.model.OpenMeteoAirQualityResponse
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

interface AirQualityApi {
    suspend fun fetchAqi(lat: Double, lon: Double): Int?
}

class OpenMeteoAirQualityApi(
    private val client: OkHttpClient = OkHttpClient(),
) : AirQualityApi {

    private val gson = Gson()

    override suspend fun fetchAqi(lat: Double, lon: Double): Int? = withContext(Dispatchers.IO) {
        val url = "https://air-quality-api.open-meteo.com/v1/air-quality".toHttpUrl()
            .newBuilder()
            .addQueryParameter("latitude", lat.toString())
            .addQueryParameter("longitude", lon.toString())
            .addQueryParameter("current", "us_aqi")
            .build()

        val response = client.newCall(Request.Builder().url(url).build()).execute()
        if (!response.isSuccessful) error("Air quality request failed with HTTP ${response.code}")
        val body = response.body?.string() ?: error("Empty response from air quality endpoint")
        gson.fromJson(body, OpenMeteoAirQualityResponse::class.java).current?.usAqi
    }
}
