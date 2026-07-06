package com.fossisawesome.ventus.work

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

const val BACKGROUND_REFRESH_WORK_NAME = "weather_background_refresh"

interface BackgroundRefreshScheduler {
    fun schedule(intervalMinutes: Int)
    fun cancel()
}

class WorkManagerBackgroundRefreshScheduler(private val context: Context) : BackgroundRefreshScheduler {

    override fun schedule(intervalMinutes: Int) {
        val request = PeriodicWorkRequestBuilder<WeatherRefreshWorker>(intervalMinutes.toLong(), TimeUnit.MINUTES).build()
        // UPDATE (not KEEP) so changing the interval replaces the existing periodic work rather
        // than stacking a second one alongside it.
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(BACKGROUND_REFRESH_WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    override fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(BACKGROUND_REFRESH_WORK_NAME)
    }
}
