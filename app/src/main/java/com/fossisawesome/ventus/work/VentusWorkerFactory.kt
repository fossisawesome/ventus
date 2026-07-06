package com.fossisawesome.ventus.work

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.fossisawesome.ventus.data.location.LocationSource
import com.fossisawesome.ventus.data.repository.LocationRepository
import com.fossisawesome.ventus.data.repository.WeatherRepository
import com.fossisawesome.ventus.data.storage.AppPreferences

// Manual DI (no Hilt) means WorkManager can't construct WeatherRefreshWorker with its
// repository/prefs dependencies via its own default no-arg factory — this supplies them from
// VentusApplication's existing singletons instead.
class VentusWorkerFactory(
    private val weatherRepository: WeatherRepository,
    private val locationRepository: LocationRepository,
    private val locationSource: LocationSource,
    private val prefs: AppPreferences,
) : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters,
    ): ListenableWorker? = when (workerClassName) {
        WeatherRefreshWorker::class.java.name ->
            WeatherRefreshWorker(appContext, workerParameters, weatherRepository, locationRepository, locationSource, prefs)
        else -> null
    }
}
