package com.fossisawesome.ventus

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fossisawesome.ventus.ui.navigation.AppNavGraph
import com.fossisawesome.ventus.ui.theme.VentusTheme
import com.fossisawesome.ventus.ui.theme.allThemes
import com.fossisawesome.ventus.ui.theme.deleteImportedTheme
import com.fossisawesome.ventus.ui.theme.importThemeFromUri
import com.fossisawesome.ventus.viewmodel.SettingsViewModel
import com.fossisawesome.ventus.viewmodel.WeatherViewModel
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val app get() = application as VentusApplication

    // Set from inside setContent once SettingsViewModel exists; read by the launcher callback.
    private var onThemeUriPicked: ((Uri) -> Unit)? = null

    private val themeImportLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { onThemeUriPicked?.invoke(it) } }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* No-op: WeatherViewModel checks permission itself via LocationSource each time the
          user taps "Use my location", so a later grant is picked up on the next tap. */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        val countryCode = resources.configuration.locales[0].country.ifBlank { Locale.getDefault().country }

        setContent {
            val weatherViewModel: WeatherViewModel = viewModel(
                factory = viewModelFactory {
                    WeatherViewModel(app.weatherRepository, app.locationSource, app.geocodingApi, app.prefs, countryCode)
                }
            )
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = viewModelFactory {
                    SettingsViewModel(
                        prefs = app.prefs,
                        loadThemes = { allThemes(applicationContext) },
                        importTheme = { uriString -> importThemeFromUri(applicationContext, Uri.parse(uriString)) },
                        deleteTheme = { file -> deleteImportedTheme(applicationContext, file) },
                    )
                }
            )

            // Registered once per composition; importThemeFile() re-reads availableThemes on success.
            onThemeUriPicked = { uri -> settingsViewModel.importThemeFile(uri.toString()) }

            LaunchedEffect(Unit) { weatherViewModel.loadInitial() }

            val themeId by settingsViewModel.themeId.collectAsStateWithLifecycle()
            val fontFamily by settingsViewModel.fontFamily.collectAsStateWithLifecycle()

            VentusTheme(themeId = themeId, fontFamily = fontFamily) {
                AppNavGraph(
                    weatherViewModel = weatherViewModel,
                    settingsViewModel = settingsViewModel,
                    onImportTheme = { themeImportLauncher.launch("*/*") },
                )
            }
        }
    }

    private inline fun <VM : ViewModel> viewModelFactory(crossinline creator: () -> VM) =
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = creator() as T
        }
}
