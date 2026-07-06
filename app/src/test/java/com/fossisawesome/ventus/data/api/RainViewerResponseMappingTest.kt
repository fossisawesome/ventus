package com.fossisawesome.ventus.data.api

import com.fossisawesome.ventus.data.model.RainViewerRadar
import com.fossisawesome.ventus.data.model.RainViewerResponse
import com.fossisawesome.ventus.data.model.toRadarFrames
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Test

class RainViewerResponseMappingTest {

    @Test
    fun `maps host and past frames into RadarFrame list`() {
        val json = """
            {
              "host": "https://tilecache.rainviewer.com",
              "radar": {
                "past": [
                  {"time": 1783348800, "path": "/v2/radar/62daa07adf10"},
                  {"time": 1783349400, "path": "/v2/radar/62daa08bcd21"}
                ],
                "nowcast": []
              }
            }
        """.trimIndent()

        val response = Gson().fromJson(json, RainViewerResponse::class.java)
        val frames = response.toRadarFrames()

        assertEquals(2, frames.size)
        assertEquals(1783348800L, frames[0].timeEpochSeconds)
        assertEquals("/v2/radar/62daa07adf10", frames[0].path)
        assertEquals("https://tilecache.rainviewer.com", frames[0].host)
    }

    @Test
    fun `appends non-empty nowcast frames after past frames`() {
        val json = """
            {
              "host": "https://tilecache.rainviewer.com",
              "radar": {
                "past": [{"time": 1783348800, "path": "/v2/radar/aaa"}],
                "nowcast": [{"time": 1783349400, "path": "/v2/radar/bbb"}]
              }
            }
        """.trimIndent()

        val frames = Gson().fromJson(json, RainViewerResponse::class.java).toRadarFrames()

        assertEquals(2, frames.size)
        assertEquals("/v2/radar/aaa", frames[0].path)
        assertEquals("/v2/radar/bbb", frames[1].path)
    }

    @Test
    fun `missing nowcast field does not crash`() {
        val json = """
            {
              "host": "https://tilecache.rainviewer.com",
              "radar": { "past": [{"time": 1, "path": "/x"}] }
            }
        """.trimIndent()

        val frames = Gson().fromJson(json, RainViewerResponse::class.java).toRadarFrames()

        assertEquals(1, frames.size)
    }
}
