package com.fossisawesome.ventus.data.api

import com.fossisawesome.ventus.data.model.GeocodingResult
import com.fossisawesome.ventus.data.model.GeocodingSearchResponse
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

interface GeocodingApi {
    suspend fun search(query: String): List<GeocodingResult>
}

class OpenMeteoGeocodingApi(
    private val client: OkHttpClient = OkHttpClient(),
) : GeocodingApi {

    private val gson = Gson()

    override suspend fun search(query: String): List<GeocodingResult> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val url = "https://geocoding-api.open-meteo.com/v1/search".toHttpUrl()
            .newBuilder()
            .addQueryParameter("name", query)
            .addQueryParameter("count", "10")
            .addQueryParameter("language", "en")
            .addQueryParameter("format", "json")
            .build()

        val response = client.newCall(Request.Builder().url(url).build()).execute()
        if (!response.isSuccessful) error("Geocoding request failed with HTTP ${response.code}")
        val body = response.body?.string() ?: error("Empty response from geocoding endpoint")
        gson.fromJson(body, GeocodingSearchResponse::class.java).results ?: emptyList()
    }
}
