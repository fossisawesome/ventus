package com.fossisawesome.ventus.data.model

// A single RainViewer radar frame, ready to build a tile URL from directly (host + path both
// carried on the frame itself, rather than returned as a separate value from the API call) —
// keeps RainViewerApi.fetchFrames()'s return type a plain List<RadarFrame> with no wrapper type.
data class RadarFrame(
    val timeEpochSeconds: Long,
    val path: String,
    val host: String,
)

// Raw shape of https://api.rainviewer.com/public/weather-maps.json — kept separate from
// RadarFrame (the domain type) the same way GeocodingSearchResponse/GeocodingResult are kept
// separate in WeatherModels.kt.
data class RainViewerResponse(
    val host: String,
    val radar: RainViewerRadar,
)

data class RainViewerRadar(
    val past: List<RainViewerFrame>,
    val nowcast: List<RainViewerFrame>?,
)

data class RainViewerFrame(
    val time: Long,
    val path: String,
)

fun RainViewerResponse.toRadarFrames(): List<RadarFrame> =
    (radar.past + (radar.nowcast ?: emptyList())).map {
        RadarFrame(timeEpochSeconds = it.time, path = it.path, host = host)
    }
