package com.fossisawesome.ventus.data.api

import com.fossisawesome.ventus.data.model.RadarFrame
import com.fossisawesome.ventus.data.model.RainViewerResponse
import com.fossisawesome.ventus.data.model.toRadarFrames
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

interface RainViewerApi {
    suspend fun fetchFrames(): List<RadarFrame>
}

class HttpRainViewerApi(
    private val client: OkHttpClient = OkHttpClient(),
) : RainViewerApi {

    private val gson = Gson()

    override suspend fun fetchFrames(): List<RadarFrame> = withContext(Dispatchers.IO) {
        val request = Request.Builder().url("https://api.rainviewer.com/public/weather-maps.json").build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) error("RainViewer request failed with HTTP ${response.code}")
        val body = response.body?.string() ?: error("Empty response from RainViewer")
        gson.fromJson(body, RainViewerResponse::class.java).toRadarFrames()
    }
}
