# Ventus

<img src="assets/logo.svg" width="96" height="96" alt="Ventus logo">

A native Kotlin/Jetpack Compose weather app for Android. Current conditions, hourly and 7-day
forecast, powered by [Open-Meteo](https://open-meteo.com/) (no API key required).

Ventus uses a custom, non-Material3 theming system: a small set of color tokens, 19 built-in
themes, user-importable `.toml` themes, and a font picker — all via `CompositionLocal`, with no
`MaterialTheme` anywhere in the app.

## Features

- Current conditions, 24-hour hourly strip, 7-day forecast
- GPS auto-detected location, with manual city search as a fallback or override
- Auto units by locale (metric/imperial), with a manual override in Settings
- Pull-to-refresh, with the last successful forecast cached for offline viewing
- 19 built-in themes + `.toml` theme import
- User-selectable font (Inter, monospace variants, system fonts, etc.)

## Installation

<a href="https://apps.obtainium.imranr.dev/redirect?r=obtainium://app/%7B%22id%22%3A%22com.fossisawesome.ventus%22%2C%22url%22%3A%22https%3A%2F%2Fgithub.com%2Ffossisawesome%2Fventus%22%2C%22author%22%3A%22fossisawesome%22%2C%22name%22%3A%22Ventus%22%2C%22preferredApkIndex%22%3A0%2C%22additionalSettings%22%3A%22%7B%5C%22includePrereleases%5C%22%3Afalse%2C%5C%22fallbackToOlderReleases%5C%22%3Atrue%2C%5C%22autoApkFilterByArch%5C%22%3Atrue%2C%5C%22versionDetection%5C%22%3Atrue%2C%5C%22sortMethodChoice%5C%22%3A%5C%22date%5C%22%7D%22%2C%22overrideSource%22%3Anull%7D"><img src="https://raw.githubusercontent.com/ImranR98/Obtainium/main/assets/graphics/badge_obtainium.png" alt="Get it on Obtainium" height="54"></a>

Or download the latest `.apk` from the [releases page](https://github.com/fossisawesome/ventus/releases/latest) and install it manually:

```bash
# Via ADB (sideloading):
adb install ventus_*.apk
```

Or transfer the APK to your device and open it with a file manager. You may need to enable
**Install from unknown sources** in your device settings.

## Requirements

- Android Studio (or the command-line Gradle wrapper) with an Android SDK installed
- `minSdk 26` (Android 8.0+), `compileSdk`/`targetSdk 36`
- Location permission (`ACCESS_COARSE_LOCATION`) for GPS-based weather; not required if you only
  use manual city search

## Building

```bash
./gradlew assembleDebug     # debug APK
./gradlew installDebug      # build and install on a connected device/emulator
./gradlew testDebugUnitTest # run the unit test suite
```

No API keys or secrets are needed — Open-Meteo's forecast and geocoding endpoints are free and
keyless.

## Project Structure

```
app/src/main/java/com/fossisawesome/ventus/
├── ui/
│   ├── theme/          # AppColors, AppTheme (19 built-ins), .toml import, font picker
│   ├── components/     # Custom Text/IconButton/Toggle/Divider/Spinner/TextButton (no Material3)
│   ├── navigation/      # AppNavGraph — "main" / "settings" routes
│   └── screens/         # MainScreen, SettingsScreen
├── data/
│   ├── api/             # Open-Meteo forecast + geocoding clients (OkHttp + Gson)
│   ├── model/           # Response models + WeatherSnapshot/WeatherUiState domain types
│   ├── repository/      # WeatherRepository — fetch, cache, offline fallback
│   ├── location/        # Fused location provider wrapper
│   ├── storage/         # DataStore-backed AppPreferences
│   └── UnitConversions.kt
├── viewmodel/            # WeatherViewModel, SettingsViewModel
├── VentusApplication.kt  # Manual DI container
└── MainActivity.kt
```

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md).

## Security

See [SECURITY.md](SECURITY.md) for how to report a vulnerability.

## License

GPL-3.0. See [LICENSE](LICENSE).
