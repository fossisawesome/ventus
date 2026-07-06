# CLAUDE.md

**Version**: 0.4.1

Start every conversation: invoke `/caveman` and `/using-superpowers` skill.

Guidance for Claude Code (claude.ai/code) when working in this repository.

Adding a new user-facing feature: add to `FEATURES.md`. Removing or significantly changing a
feature: update that file. Keep entries user-focused (no code references or internal names).

## Project Overview

**Ventus** is a native Kotlin/Jetpack Compose weather app for Android. Single active location
(GPS-detected, manual search override), current conditions + 24h hourly strip + 7-day forecast,
powered by [Open-Meteo](https://open-meteo.com/) (keyless).

### Tech Stack

- **UI**: Jetpack Compose, no `material3` widgets — custom primitives in `ui/components/Widgets.kt`
  (`material-icons-extended` IS used, for `Icons.Default.*` glyphs only)
- **Language**: Kotlin, `minSdk 26`, `compileSdk`/`targetSdk 36`
- **Networking**: OkHttp + Gson (no Retrofit), synchronous `execute()` inside `withContext(Dispatchers.IO)`
- **Persistence**: `androidx.datastore:datastore-preferences`
- **Location**: Google Play Services fused location provider
- **State management**: MVVM — `StateFlow` + `lifecycle-viewmodel-compose`, manual DI via `VentusApplication`
- **Navigation**: `androidx.navigation:navigation-compose`, two routes (`"main"`, `"settings"`)

## Architecture

```
app/src/main/java/com/fossisawesome/ventus/
  VentusApplication.kt      - manual DI container: lazy prefs/api/repository/location singletons
  MainActivity.kt           - hosts VentusTheme + AppNavGraph
  ui/
    theme/
      AppColors.kt          - AppColors data class (bg, surface, surface2, text, muted, accent, error
                              + computed border), CompositionLocals (LocalAppColors, LocalAppIsDark,
                              LocalAppFontFamily)
      AppTheme.kt            - AppTheme data class, ALL_THEMES (19 built-ins), DEFAULT_THEME_ID = "ventus",
                              themeById(), VentusTheme() composable
      ThemeImport.kt         - hand-rolled .toml parser/import/delete, filesDir/themes/, 50KB cap
      AppFont.kt             - AppFontKey enum + FONT_OPTIONS + fontKeyFor()
    components/
      Widgets.kt             - Text, AppIcon, IconButton, Toggle, Divider, Spinner, TextButton
    navigation/
      AppNavGraph.kt         - NavHost, "main"/"settings" routes
    screens/
      MainScreen.kt          - current/hourly/daily forecast, pull-to-refresh, empty/error states
      SettingsScreen.kt      - theme grid, font list, units toggle, location controls
  data/
    UnitConversions.kt       - pure C↔F, km/h↔mph, mm↔in + weather-code → description/icon
    api/
      WeatherApi.kt          - WeatherApi interface + OpenMeteoWeatherApi (OkHttp+Gson), mapForecastResponse()
      GeocodingApi.kt        - GeocodingApi interface + OpenMeteoGeocodingApi
    model/
      WeatherModels.kt       - Gson response models + domain WeatherSnapshot/WeatherUiState
    repository/
      WeatherRepository.kt   - refresh() (fetch → cache-on-success, cache-fallback-on-failure), loadCached()
    location/
      LocationSource.kt      - interface + FusedLocationSource wrapper
    storage/
      AppPreferences.kt      - DataStore: themeId, fontFamily, unitsMode, location, cached weather JSON
  viewmodel/
    WeatherViewModel.kt      - StateFlow<WeatherUiState>, drives MainScreen
    SettingsViewModel.kt     - theme/font/units/import state, drives SettingsScreen
```

### Data flow

```
ui/screens/            (Compose UI)
    │  user action
    ▼
viewmodel/WeatherViewModel or SettingsViewModel  (StateFlow mutation, coroutine launch)
    │
    ▼
data/repository/WeatherRepository
    ├─ data/api/WeatherApi (OpenMeteoWeatherApi) ──► Open-Meteo forecast endpoint
    │       └─ mapForecastResponse() → WeatherSnapshot
    ├─ data/storage/AppPreferences ──► DataStore (cache read/write, on every refresh)
    └─ on failure: falls back to cached WeatherSnapshot → WeatherUiState.Stale
    │  WeatherUiState (Success/Stale/Error/NeedsLocation)
    ▼
StateFlow emits ──► Compose recomposes MainScreen/SettingsScreen
```

`WeatherUiState` variants: `NeedsLocation`, `Loading`, `Success(snapshot)`, `Stale(snapshot, fetchedAt)`,
`Error(message)`. `WeatherSnapshot` always stores SI units (Celsius, km/h, mm) regardless of the
user's display preference — unit conversion for display happens in `UnitConversions.kt` at the UI
layer, never in the API client (`WeatherApi.kt` fetches metric on purpose — see comment there — to
avoid double-converting if display unit is also imperial).

## Build & Run

```bash
./gradlew assembleDebug       # debug APK
./gradlew installDebug        # build + install on connected device/emulator
./gradlew testDebugUnitTest   # unit test suite
```

No API keys or secrets needed — Open-Meteo's forecast and geocoding endpoints are free and keyless.

### First-Time Setup

1. Clone repo; ensure Android SDK + Gradle wrapper work (`./gradlew --version`)
2. `./gradlew installDebug` on a connected device/emulator
3. Grant location permission on first launch, or use manual city search

## Development Notes

### Adding a UI Action / Backend Call

- `WeatherViewModel`/`SettingsViewModel` expose `StateFlow`s consumed via `collectAsStateWithLifecycle()`
  in `AppNavGraph.kt`, then passed down as plain params + lambdas to `MainScreen`/`SettingsScreen`.
- Repository/API calls are `suspend fun`s launched from the ViewModel's `viewModelScope`.
- New screens: add a route in `AppNavGraph.kt`, wire it the same way as `"main"`/`"settings"`.

### Theme System

- Never use `MaterialTheme`, `androidx.compose.material3.*` widgets, or `Icons.Default.*` outside
  `AppIcon` — all color access goes through `LocalAppColors.current`.
- New built-in theme: add an `AppTheme(...)` entry to `ALL_THEMES` in `AppTheme.kt` with all 7
  hex colors (`bg, surface, surface2, text, muted, accent, error`).
- Imported themes: `.toml` files under `filesDir/themes/` — don't change the parser's expected keys
  (`name`, `color_scheme`, `[colors]` table) without a clear reason, existing theme files depend on them.

### Units

- `WeatherSnapshot` is always stored/cached in SI (Celsius, km/h, mm). Display-time conversion only,
  in `UnitConversions.kt`. `unitsMode` is `"auto"` (locale-based) / `"metric"` / `"imperial"`.

### Debugging

- `Log.d`/`e` to logcat; `adb logcat | grep Ventus` while `./gradlew installDebug` is running.
- Network issues: check `WeatherApi.kt`/`GeocodingApi.kt` — both throw `error(...)` (an
  `IllegalStateException`) on non-2xx or empty body, caught by `WeatherRepository.refresh()`.

## Testing

Unit tests live under `app/src/test/java/com/fossisawesome/ventus/`, no instrumented/UI tests for v1.
Run:

```bash
./gradlew testDebugUnitTest
```

Covers: unit conversion helpers, `.toml` theme parser, `WeatherResponse` → domain model mapping,
`WeatherRepository` cache fallback behavior, `WeatherViewModel`/`SettingsViewModel` orchestration.

## Key Files

- `MainActivity.kt` — entry point, hosts `VentusTheme` + `AppNavGraph`
- `VentusApplication.kt` — manual DI container (no Hilt/Koin)
- `ui/theme/` — CompositionLocal-based theming, 19 built-in themes + `.toml` import
- `ui/components/Widgets.kt` — all custom UI primitives, no Material3
- `data/repository/WeatherRepository.kt` — fetch/cache/fallback core logic
- `data/storage/AppPreferences.kt` — DataStore keys and flows

## OpenMeteo API Integration

See [API.md](API.md) for full reference of the Open-Meteo forecast and geocoding endpoints used.

## Versioning

- Always use semantic versioning.

## Comments

- Add comment above new code only when WHY is non-obvious (hidden constraint, workaround, subtle
  invariant). Well-named code doesn't need WHAT explained.
- Use existing comments to understand surrounding code (e.g. `WeatherApi.kt`'s metric-fetch comment).

# Foundational Thinking Principles

Apply to all interactions: conversations, code, debugging, planning, anything.

## 1. Think Before Acting

State assumptions explicitly. Uncertain: name it. Multiple interpretations: present them. Simpler
approach exists: mention it. Something unclear: stop, ask.

## 2. Simplicity First

Minimum code that solves the problem. No features beyond what was asked. No abstractions for
single-use code. No error handling for impossible scenarios.

## 3. Surgical Changes

Touch only what you must. Don't "improve" adjacent code. Match existing style. Every changed line
should trace directly to the request.

## 4. Goal-Driven Execution

Define success criteria before starting. For example: "Fix forecast parsing bug" → write/adjust a
test in `WeatherResponseMappingTest.kt` that reproduces it, then make it pass.

## 5. Verify, Don't Assume

Check Gradle/dependency versions before recommending changes. Verify Android API level constraints
(`minSdk 26`) before using newer platform APIs.

---

**For autonomous tool use and multi-step workflows, see `AGENTS.md`.**

## Meta: Guidelines Are Defaults, Not Laws

If the user says "I want this abstracted" or "performance matters more than simplicity here," that
overrides guidelines above. Your judgment always wins when direction is explicit.

## Dependencies

Research/web search dependencies before adding them — confirm still maintained/safe, avoid dependency
if a small amount of in-repo code would do (exceptions where reimplementing would be genuinely
unmaintainable, e.g. `OkHttp`/`Gson`/fused-location).

## Questioning

Ask and interrogate before: adding features, changing UI, non-trivial debugging.
