package com.fossisawesome.ventus

import android.app.Application
import androidx.work.Configuration
import com.fossisawesome.ventus.data.api.NwsWeatherApi
import com.fossisawesome.ventus.data.api.OpenMeteoAirQualityApi
import com.fossisawesome.ventus.data.api.OpenMeteoGeocodingApi
import com.fossisawesome.ventus.data.api.OpenMeteoWeatherApi
import com.fossisawesome.ventus.data.location.FusedLocationSource
import com.fossisawesome.ventus.data.repository.LocationRepository
import com.fossisawesome.ventus.data.repository.WeatherRepository
import com.fossisawesome.ventus.data.storage.AppPreferences
import com.fossisawesome.ventus.work.BackgroundRefreshScheduler
import com.fossisawesome.ventus.work.VentusWorkerFactory
import com.fossisawesome.ventus.work.WorkManagerBackgroundRefreshScheduler
import com.fossisawesome.ventus.widget.GlanceWidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// Manual DI container — holds app-wide singletons shared across ViewModels.
class VentusApplication : Application(), Configuration.Provider {
    val prefs by lazy { AppPreferences(this) }
    val weatherApi by lazy { OpenMeteoWeatherApi() }
    val nwsWeatherApi by lazy { NwsWeatherApi() }
    val airQualityApi by lazy { OpenMeteoAirQualityApi() }
    val geocodingApi by lazy { OpenMeteoGeocodingApi() }
    val weatherRepository by lazy { WeatherRepository(weatherApi, nwsWeatherApi, airQualityApi, prefs) }
    val locationRepository by lazy { LocationRepository(prefs) }
    val locationSource by lazy { FusedLocationSource(this) }
    val backgroundRefreshScheduler: BackgroundRefreshScheduler by lazy { WorkManagerBackgroundRefreshScheduler(this) }
    val widgetUpdater by lazy { GlanceWidgetUpdater(this) }

    // WorkManager needs a WorkerFactory that knows how to construct WeatherRefreshWorker with its
    // repository/prefs dependencies (manual DI, no Hilt) — implementing Configuration.Provider
    // switches WorkManager to on-demand initialization using THIS configuration instead of its
    // default no-arg-constructor factory (paired with removing the default initializer in the
    // manifest — see AndroidManifest.xml).
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(VentusWorkerFactory(weatherRepository, locationRepository, locationSource, prefs, widgetUpdater))
            .build()

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        // Reconciles WorkManager's scheduled state with persisted prefs on every process start —
        // covers both a normal cold start (prefs already matched, this is a harmless no-op re-
        // enqueue with the same interval) and the case where prefs changed while the app's
        // process was dead or WorkManager's own state was cleared (e.g. user cleared app data).
        applicationScope.launch {
            if (prefs.backgroundRefreshEnabled.first()) {
                backgroundRefreshScheduler.schedule(prefs.backgroundRefreshIntervalMinutes.first())
            } else {
                backgroundRefreshScheduler.cancel()
            }
        }
    }
}
