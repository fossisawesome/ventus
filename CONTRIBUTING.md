# Contributing to Ventus

Thanks for your interest in contributing! Ventus is a native Kotlin/Jetpack Compose weather app
for Android. This guide will help you get started.

## Quick Start

### Prerequisites
- Android Studio (or the command-line Gradle wrapper) with an Android SDK installed
- A device or emulator running Android 8.0 (API 26) or later

### Set Up Your Environment

```bash
# Clone the repo
git clone https://github.com/fossisawesome/ventus
cd ventus

# Build and install on a connected device/emulator
./gradlew installDebug
```

No API keys, accounts, or secrets are needed — Open-Meteo's forecast and geocoding endpoints are
free and keyless.

## Project Structure

```
app/src/main/java/com/fossisawesome/ventus/
├── ui/
│   ├── theme/           # AppColors, AppTheme (19 built-ins), .toml import, font picker
│   ├── components/      # Custom Text/IconButton/Toggle/Divider/Spinner/TextButton (no Material3)
│   ├── navigation/       # AppNavGraph — "main" / "settings" routes
│   └── screens/          # MainScreen, SettingsScreen
├── data/
│   ├── api/              # Open-Meteo forecast + geocoding clients (OkHttp + Gson)
│   ├── model/             # Response models + WeatherSnapshot/WeatherUiState domain types
│   ├── repository/        # WeatherRepository — fetch, cache, offline fallback
│   ├── location/           # Fused location provider wrapper
│   ├── storage/            # DataStore-backed AppPreferences
│   └── UnitConversions.kt
├── viewmodel/              # WeatherViewModel, SettingsViewModel
├── VentusApplication.kt    # Manual DI container (no Hilt/Dagger)
└── MainActivity.kt
```

**Key principle**: no `MaterialTheme` or Material3 components anywhere in the app. All UI is built
on custom primitives in `ui/components/` layered over plain Compose Foundation, styled via the
`AppColors`/`AppTheme` `CompositionLocal`s in `ui/theme/`.

## Development Workflow

- `WeatherViewModel` and `SettingsViewModel` take their dependencies (API clients, location
  source, theme load/import/delete) as constructor parameters or injected lambdas — this is what
  makes them unit-testable without Robolectric or a real `Context`. If you add a dependency that
  needs Android platform types (`Context`, `Uri`, etc.), prefer wrapping it the same way rather
  than threading the platform type through the view model.
- Domain models (`WeatherSnapshot`, `HourlyPoint`, `DailyPoint`) always store temperature/wind/
  precipitation in SI units (Celsius, km/h, mm), regardless of the user's display unit
  preference — conversion for display happens only in the UI layer
  (`data/UnitConversions.kt`). Don't have the API layer request Fahrenheit/mph directly; that
  causes double conversion once the UI converts on top.
- The theme system (colors, `.toml` import format) has stable keys (`name`, `color_scheme`,
  `[colors]` table) — existing theme files depend on them, so don't change the parsing logic
  without a clear reason.

### Testing

Run the unit test suite:

```bash
./gradlew testDebugUnitTest
```

Unit tests cover pure logic and orchestration: unit conversions, weather-code mapping, the `.toml`
theme parser, API response mapping, `WeatherRepository`'s cache fallback, and the view models
(using hand-written fakes for the API/location/theme dependencies).

There is no instrumented/UI test suite. For UI or behavioral changes, manually test on a device
or emulator:

1. `./gradlew installDebug` and launch the app
2. Grant (or deny) location permission and confirm both paths work
3. Pull-to-refresh, and confirm the offline/stale-cache banner appears when the network is
   unavailable
4. Open Settings, switch themes/fonts/units, and confirm the whole app re-skins live
5. Search for a city and confirm results and selection work
6. Check `adb logcat` for exceptions during all of the above

## Code Style & Conventions

- **Simplicity first**: no speculative abstractions or premature optimization
- **Surgical changes**: touch only what's needed; don't refactor unrelated code
- **Comments**: only when the WHY is non-obvious (a workaround, a constraint, a subtle invariant)
- Follow the existing MVVM + manual-DI pattern (`VentusApplication` as the singleton container) —
  don't introduce Hilt/Dagger/Koin for a project this size
- Match the existing custom-widget idiom in `ui/components/Widgets.kt` rather than reaching for
  Material3 components

## Submitting Changes

### Before You Start
1. Check if an issue or discussion exists for your idea
2. For major changes, open an issue or discussion first to align on approach
3. Fork the repo and create a feature branch: `git checkout -b feature/your-feature-name`

### Making Your Change
1. Write code following the conventions above
2. Add or update unit tests for any new logic in `data/`, `viewmodel/`, or `ui/theme/`
3. Run `./gradlew testDebugUnitTest` and `./gradlew assembleDebug` — both must succeed
4. Manually test the affected flow on a device/emulator (see Testing above)
5. Commit with a clear message: "Add X feature" or "Fix Y bug"

### Submitting a PR
1. Push your branch and open a PR against `main`
2. Include a clear description of what changed and why
3. Reference any related issues
4. Wait for feedback — maintainers review for correctness and alignment with project conventions

### PR Expectations
- Code follows the conventions above
- Changes are focused (one feature or fix per PR, not a grab-bag)
- Unit tests are included for new logic; manual test steps are described for UI changes
- Commit history is clean (use atomic commits)

## Getting Help

- **Bug reports**: open an issue with reproduction steps and device/emulator details
- **Feature requests**: open a discussion or issue describing the use case

## License

By contributing, you agree that your changes are licensed under the [GPL-3.0](LICENSE) license,
the same as the project.

---

Happy coding! If anything is unclear, open an issue or discussion.
