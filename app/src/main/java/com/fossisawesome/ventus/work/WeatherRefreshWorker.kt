package com.fossisawesome.ventus.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fossisawesome.ventus.data.location.LocationSource
import com.fossisawesome.ventus.data.repository.LocationRepository
import com.fossisawesome.ventus.data.repository.WeatherRepository
import com.fossisawesome.ventus.data.storage.AppPreferences
import java.util.Locale

// Thin WorkManager wrapper around refreshAllLocations() — all the actual refresh logic (and its
// test coverage) lives there; this class exists only because WorkManager needs a CoroutineWorker
// subclass to schedule periodic work.
class WeatherRefreshWorker(
    context: Context,
    params: WorkerParameters,
    private val weatherRepository: WeatherRepository,
    private val locationRepository: LocationRepository,
    private val locationSource: LocationSource,
    private val prefs: AppPreferences,
    private val widgetUpdater: WidgetUpdater,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val countryCode = applicationContext.resources.configuration.locales[0].country
            .ifBlank { Locale.getDefault().country }
        refreshAllLocations(weatherRepository, locationRepository, locationSource, prefs, countryCode)
        widgetUpdater.notifyActiveLocationChanged()
        // Per-location failures are already absorbed into Stale/Error cache states by
        // WeatherRepository.refresh() — there's nothing left for WorkManager's own retry
        // mechanism to usefully retry, so this always reports success.
        return Result.success()
    }
}
