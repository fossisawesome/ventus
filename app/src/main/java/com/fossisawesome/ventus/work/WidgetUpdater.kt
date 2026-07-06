package com.fossisawesome.ventus.work

// Tells the home-screen widget to redraw with fresh cache data. The real implementation calls
// Glance's VentusWidget().updateAll(context); this interface exists so WeatherViewModel and
// WeatherRefreshWorker don't need a real Glance runtime to be unit-tested — fakes are used instead,
// same pattern as BackgroundRefreshScheduler.
interface WidgetUpdater {
    suspend fun notifyActiveLocationChanged()
}
