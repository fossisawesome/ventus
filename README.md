# Ventus

A native Kotlin/Jetpack Compose weather app for Android. Current conditions, hourly and 7-day
forecast, powered by [Open-Meteo](https://open-meteo.com/) (no API key required).

Ventus follows the same custom, non-Material3 theming system as its sibling project,
[Firmium](https://github.com/fossisawesome/firmium): a small set of color tokens, 19 built-in
themes (18 shared with Firmium, plus a Ventus-specific default), user-importable `.toml` themes,
and a shared font picker — all via `CompositionLocal`, with no `MaterialTheme` anywhere in the app.

## Features

- Current conditions, 24-hour hourly strip, 7-day forecast
- GPS auto-detected location, with manual city search as a fallback or override
- Auto units by locale (metric/imperial), with a manual override in Settings
- Pull-to-refresh, with the last successful forecast cached for offline viewing
- 19 built-in themes + `.toml` theme import (cross-compatible with Firmium's theme files)
- User-selectable font (Inter, monospace variants, system fonts, etc.)

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
