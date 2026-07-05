package com.fossisawesome.ventus

import android.app.Application
import com.fossisawesome.ventus.data.api.OpenMeteoGeocodingApi
import com.fossisawesome.ventus.data.api.OpenMeteoWeatherApi
import com.fossisawesome.ventus.data.location.FusedLocationSource
import com.fossisawesome.ventus.data.repository.WeatherRepository
import com.fossisawesome.ventus.data.storage.AppPreferences

// Manual DI container — holds app-wide singletons shared across ViewModels.
class VentusApplication : Application() {
    val prefs by lazy { AppPreferences(this) }
    val weatherApi by lazy { OpenMeteoWeatherApi() }
    val geocodingApi by lazy { OpenMeteoGeocodingApi() }
    val weatherRepository by lazy { WeatherRepository(weatherApi, prefs) }
    val locationSource by lazy { FusedLocationSource(this) }
}
