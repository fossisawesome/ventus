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
