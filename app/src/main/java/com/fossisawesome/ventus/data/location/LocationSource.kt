package com.fossisawesome.ventus.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

sealed interface LocationResult {
    data class Success(val lat: Double, val lon: Double) : LocationResult
    data object PermissionDenied : LocationResult
    data object Unavailable : LocationResult
}

interface LocationSource {
    suspend fun getCurrentLocation(): LocationResult
}

fun hasLocationPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED

class FusedLocationSource(private val context: Context) : LocationSource {

    private val client = LocationServices.getFusedLocationProviderClient(context)

    override suspend fun getCurrentLocation(): LocationResult {
        if (!hasLocationPermission(context)) return LocationResult.PermissionDenied

        return suspendCancellableCoroutine { cont: CancellableContinuation<LocationResult> ->
            try {
                client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                    .addOnSuccessListener { location ->
                        if (location != null) {
                            cont.resume(LocationResult.Success(location.latitude, location.longitude))
                        } else {
                            cont.resume(LocationResult.Unavailable)
                        }
                    }
                    .addOnFailureListener {
                        cont.resume(LocationResult.Unavailable)
                    }
            } catch (_: SecurityException) {
                cont.resume(LocationResult.PermissionDenied)
            }
        }
    }
}

// GPS-tracked entries re-resolve their coordinates live on every refresh rather than trusting the
// last-saved lat/lon (the device may have moved since) — falls back to the last-known coordinates
// only if a fresh GPS fix can't be obtained, so a temporary GPS blip doesn't blank an otherwise-
// working page. Shared by WeatherViewModel's per-page refresh and the background refresh path
// (work/BackgroundRefresh.kt) so this rule lives in exactly one place.
suspend fun resolveLocationCoords(
    location: com.fossisawesome.ventus.data.model.Location,
    locationSource: LocationSource,
): Pair<Double, Double> {
    if (!location.isCurrentLocation) return location.lat to location.lon
    return when (val result = locationSource.getCurrentLocation()) {
        is LocationResult.Success -> result.lat to result.lon
        LocationResult.PermissionDenied, LocationResult.Unavailable -> location.lat to location.lon
    }
}
