package com.fossisawesome.ventus

import android.app.Application

// Manual DI container — holds app-wide singletons shared across ViewModels.
// Later tasks add lazy properties here (prefs, weather API, repository, location source).
class VentusApplication : Application()
