package com.fossisawesome.ventus.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.fossisawesome.ventus.data.model.Location
import com.fossisawesome.ventus.data.model.RadarFrame
import com.fossisawesome.ventus.ui.components.*
import com.fossisawesome.ventus.ui.theme.LocalAppColors
import com.fossisawesome.ventus.ui.theme.LocalAppFontFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.style.layers.RasterLayer
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.sources.RasterSource

private const val BASE_SOURCE_ID = "osm-base-source"
private const val BASE_LAYER_ID = "osm-base-layer"
private const val RADAR_SOURCE_ID = "rainviewer-radar-source"
private const val RADAR_LAYER_ID = "rainviewer-radar-layer"
// CARTO basemaps are free/keyless and explicitly meant for app consumption, unlike raw
// tile.openstreetmap.org (policy-blocks bulk app traffic, caused blank-map 403s here).
private const val BASE_TILE_URL = "https://basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}.png"

private fun radarTileUrl(frame: RadarFrame) = "${frame.host}${frame.path}/256/{z}/{x}/{y}/2/1_1.png"

@Composable
fun MapScreen(
    activeLocation: Location?,
    frames: List<RadarFrame>,
    currentFrameIndex: Int,
    isPlaying: Boolean,
    onTogglePlayback: () -> Unit,
    onAdvanceFrame: () -> Unit,
    onBack: () -> Unit,
) {
    val colors = LocalAppColors.current
    val font = LocalAppFontFamily.current
    val context = LocalContext.current

    LaunchedEffect(isPlaying, frames) {
        if (!isPlaying || frames.isEmpty()) return@LaunchedEffect
        while (isActive) {
            delay(500)
            onAdvanceFrame()
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(colors.bg)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                AppIcon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = colors.muted)
            }
            Text("Radar", color = colors.text, fontFamily = font, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Divider()

        Box(modifier = Modifier.fillMaxSize()) {
            if (activeLocation == null) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text("No location yet", color = colors.text, fontFamily = font, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Add a location on the main screen to see its radar.",
                        color = colors.muted,
                        fontFamily = font,
                    )
                }
            } else {
                RadarMapView(
                    activeLocation = activeLocation,
                    frames = frames,
                    currentFrameIndex = currentFrameIndex,
                    modifier = Modifier.fillMaxSize(),
                )

                IconButton(
                    onClick = onTogglePlayback,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                        .size(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(colors.surface),
                ) {
                    AppIcon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = colors.accent,
                    )
                }

                Text(
                    text = "Data: RainViewer.com",
                    color = colors.muted,
                    fontFamily = font,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                        .background(colors.surface, RoundedCornerShape(6.dp))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.rainviewer.com/")))
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
    }
}

// Owns the MapLibre MapView's lifecycle and style/camera setup. Camera is centered ONCE, when the
// style first loads — subsequent frame changes only swap the radar overlay's tile source (via
// remove-then-re-add, since RasterSource has no in-place tile-URL mutator in this MapLibre
// version), never re-touching the camera, so user pan/zoom is never overridden by playback.
@Composable
private fun RadarMapView(
    activeLocation: Location,
    frames: List<RadarFrame>,
    currentFrameIndex: Int,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember {
        MapLibre.getInstance(context)
        MapView(context)
    }
    var mapLibreMap by remember { mutableStateOf<MapLibreMap?>(null) }

    DisposableEffect(lifecycleOwner) {
        mapView.onCreate(null)
        mapView.getMapAsync { map ->
            mapLibreMap = map
            val style = Style.Builder()
                .withSource(RasterSource(BASE_SOURCE_ID, BASE_TILE_URL, 256))
                .withLayer(RasterLayer(BASE_LAYER_ID, BASE_SOURCE_ID))
            map.setStyle(style) {
                map.cameraPosition = CameraPosition.Builder()
                    .target(LatLng(activeLocation.lat, activeLocation.lon))
                    .zoom(8.0)
                    .build()
            }
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }

    LaunchedEffect(mapLibreMap, frames, currentFrameIndex) {
        val map = mapLibreMap ?: return@LaunchedEffect
        val frame = frames.getOrNull(currentFrameIndex) ?: return@LaunchedEffect
        val style = map.style ?: return@LaunchedEffect
        style.getLayer(RADAR_LAYER_ID)?.let { style.removeLayer(it) }
        style.getSource(RADAR_SOURCE_ID)?.let { style.removeSource(it) }
        style.addSource(RasterSource(RADAR_SOURCE_ID, radarTileUrl(frame), 256))
        style.addLayer(RasterLayer(RADAR_LAYER_ID, RADAR_SOURCE_ID))
    }

    AndroidView(factory = { mapView }, modifier = modifier)
}
