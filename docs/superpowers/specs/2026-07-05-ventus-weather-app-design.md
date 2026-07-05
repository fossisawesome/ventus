# Ventus — Native Android Weather App Design

Status: approved
Date: 2026-07-05

## Overview

Ventus is a native Kotlin/Jetpack Compose weather app for Android. It follows the
visual and theming style of the Firmium app (`/home/larp/firmium-app/firmium/android`)
rather than Material 3: a custom, CompositionLocal-based color/theme system, custom
Compose UI primitives in place of Material3 widgets, and a user-selectable font system.

v1 scope: a single current location (GPS-detected, with manual search override),
showing current conditions, an hourly forecast strip, and a 7-day forecast list on
one main screen, plus a settings screen for theme/font/units/location management.

## Goals / non-goals

**Goals (v1):**
- Current + hourly + 7-day forecast for one active location.
- GPS auto-detect location, with manual city search as fallback/override.
- Firmium-identical theming system: same 7-color token model, same 18 built-in
  themes ported verbatim, plus a new "Ventus" flagship theme, plus user `.toml`
  theme import (cross-compatible with Firmium's theme files).
- Same font-picker system as Firmium.
- Auto unit system by locale (metric/imperial) with manual override in settings.
- Pull-to-refresh, with last-successful-response caching for offline/error display.

**Non-goals (v1):**
- Multiple saved locations / swiping between cities.
- Radar/precipitation map view.
- Home screen widget.
- Background/periodic auto-refresh (WorkManager).
- Wear OS companion (Firmium has one; Ventus does not, for v1).

## Stack

- Kotlin, Jetpack Compose (Android only, not Compose Multiplatform).
- `minSdk 26`, `compileSdk 36`, `targetSdk 36` — matches Firmium.
- `namespace`/`applicationId`: `com.fossisawesome.ventus`.
- Navigation: `androidx.navigation:navigation-compose`.
- Networking: OkHttp + Gson (no Retrofit — matches Firmium's approach).
- Persistence: `androidx.datastore:datastore-preferences`.
- Location: Google Play Services fused location provider
  (`com.google.android.gms:play-services-location`).
- Coroutines + `lifecycle-viewmodel-compose` for state management (MVVM).

## Architecture

MVVM with a single `WeatherViewModel` exposing a `StateFlow<WeatherUiState>` to
`MainScreen`, and a `SettingsViewModel` for the settings screen (theme id, font,
units, location prefs) — mirrors Firmium's `viewmodel/` package pattern.

```
ui/
  theme/        - ported from Firmium: AppColors, AppTheme, ThemeImport, AppFont
  components/   - ported from Firmium: Text, IconButton, Toggle, Divider, Spinner, TextButton
  screens/      - MainScreen, SettingsScreen
  navigation/   - AppNavGraph (2 routes: "main", "settings")
viewmodel/
  WeatherViewModel.kt
  SettingsViewModel.kt
data/
  api/          - OpenMeteoApi (OkHttp client + Gson parsing), GeocodingApi
  model/        - WeatherResponse, GeocodingResult, etc. (Gson data classes)
  repository/   - WeatherRepository (fetch + cache read/write)
  location/     - LocationProvider (fused location wrapper + permission state)
  storage/      - AppPreferences (DataStore: theme id, font, units, location, cached JSON)
```

### Data flow

1. On launch, `WeatherViewModel` reads `AppPreferences` for a saved location. If
   none, and location permission is granted, it requests a fresh GPS fix; if no
   permission or no fix, it shows a "search for your city" empty state.
2. Given coordinates, `WeatherRepository` calls Open-Meteo's forecast endpoint
   (current + hourly + daily blocks in one request) and returns a parsed
   `WeatherUiState.Success`.
3. On success, the raw JSON (or a serialized subset) is written to
   `AppPreferences` as the offline cache, tagged with a fetch timestamp.
4. On failure (no network, HTTP error), the repository falls back to the cached
   value if present, wrapped as `WeatherUiState.Stale(cached, fetchedAt)`; if no
   cache exists, `WeatherUiState.Error`.
5. Pull-to-refresh re-runs the fetch for the current saved location/coordinates.
6. Manual city search (Settings, or from the main screen's location entry point)
   calls Open-Meteo's geocoding endpoint, and selecting a result overwrites the
   saved location and triggers a fresh fetch.

### APIs

- Forecast: `https://api.open-meteo.com/v1/forecast` — request current weather
  code/temp/wind, hourly temp/precip-probability, daily min/max/weather-code,
  with `temperature_unit`/`wind_speed_unit`/`precipitation_unit` params driven by
  the units setting.
- Geocoding: `https://geocoding-api.open-meteo.com/v1/search` — query by city
  name, returns candidate matches (name, admin area, country, lat/lon) for the
  user to pick from.

No API key required for either.

## Theming

Ported directly from Firmium's `ui/theme/` package:

- `AppColors` data class: `bg, surface, surface2, text, muted, accent, error`,
  with computed `border = surface2.copy(alpha = 0.4f)`.
- `AppTheme` data class (one entry per built-in): `id, name, isDark`, plus the
  7 colors, plus `isImported`/`sourceFile` for user-imported themes.
- Same 18 built-in themes as Firmium (Dracula, Tokyo Night, Catppuccin x3,
  Gruvbox, Nord, Synthwave '84, Ayu (dark/light), GitHub Dark, Adwaita
  (dark/light), Nordfox, Monokai, Svalbard, Catppuccin Latte), copied verbatim
  with identical hex values.
- **New 19th theme, "Ventus"** — this app's default/flagship theme, dark,
  weather/sky themed:
  - `bg #0d1420, surface #16202e, surface2 #1f2c3d, text #e8f0f7,`
    `muted #7891a8, accent #5ec8f0, error #f2665a`
  - `DEFAULT_THEME_ID = "ventus"`
- `ThemeImport.kt` ported verbatim: same hand-rolled `.toml` parser (`name`,
  `color_scheme`, `[colors]` table), same `filesDir/themes/*.toml` storage,
  same 50 KB size cap and sanitized-filename-on-import behavior. Theme files
  are cross-compatible between Firmium and Ventus since the format is identical.
- `AppFont.kt` ported verbatim: same `AppFontKey` enum and same font list
  (Inter, Liberation Mono, Monospace, System, Comic Sans, Sans Serif, BigBlue
  Terminal, Cousine, FiraCode, Hack). Same bundled font files copied into
  `res/font/`.
- All colors/font are provided via `CompositionLocal` (`LocalAppColors`,
  `LocalAppIsDark`, `LocalAppFontFamily`) exactly as in Firmium — no
  `MaterialTheme` anywhere in the app.

## UI components

Ported from Firmium's `FirmiumUi.kt` (renamed without the app-specific prefix,
since these aren't Firmium-branded — plain `Text`, `IconButton`, `Toggle`,
`Divider`, `Spinner`, `TextButton`, `VerticalScrollbar`):

- `Text` — `BasicText`-backed, replaces Compose Material `Text()`.
- `IconButton` — tap-target `Box` with press-scale + alpha overlay animation,
  haptic feedback on click.
- `Toggle` — animated thumb switch (used for units auto/override, etc.).
- `Divider` — 1dp rule using `border` color token.
- `Spinner` — animated arc, used for loading state.
- `TextButton` — press-feedback text button, replaces Material `TextButton()`.

No new component types are needed for weather-specific UI (hourly strip is a
`LazyRow` of plain `Column`s; daily list is a `LazyColumn` of row `Box`es) —
these are built directly from the above primitives plus base Compose layout.

## Screens

### MainScreen
- Top: location name (tap to open search/override), refresh-stale indicator
  if showing cached data.
- Current conditions block: large temperature, weather icon/description, feels
  like, humidity, wind.
- Hourly strip: horizontal `LazyRow`, next 24h, temp + icon + hour label.
- Daily list: vertical list, 7 days, day name + icon + low/high.
- Pull-to-refresh wraps the whole scrollable content.
- Empty state (no location yet): prompt + "use my location" button (triggers
  permission request) + search field.
- Error state (no cache, fetch failed): message + retry button.

### SettingsScreen
- Theme picker: grid of swatches (one per built-in + imported theme), tap to
  apply immediately; "Import theme…" row opens a file picker (`.toml`).
- Font picker: list of font display names, tap to apply immediately.
- Units: toggle group — Auto (locale-based) / Metric / Imperial.
- Location: "Use current location" (re-triggers GPS), and a search field to
  override with a manually chosen city.

Navigation: `navigation-compose` with two routes, `"main"` and `"settings"`,
gear icon on `MainScreen` navigates to `"settings"`.

## Permissions

- `ACCESS_COARSE_LOCATION` requested at first launch (or when the user taps
  "use my location"), with a rationale shown before the system prompt if
  previously denied.
- If denied (once or permanently), the app falls back to the manual search
  empty state — never blocks app usage on the permission being granted.

## Error handling / offline behavior

- Every fetch attempt is wrapped in a `runCatching`-style result at the
  repository layer; failures never crash the UI, they map to `Stale` or
  `Error` UI states.
- `Stale` state renders the same layout as `Success` but with a small banner:
  "Updated Xm/Xh ago — pull to refresh".
- `Error` state (no cache available) is a simple centered message + retry
  button — no partial/garbled rendering.

## Testing

- Unit tests:
  - Unit conversion helpers (C↔F, km/h↔mph, mm↔in) — pure functions, easy to
    verify exhaustively.
  - `.toml` theme parser/import (mirrors Firmium's existing test coverage
    for `ThemeImport.kt`).
  - `WeatherRepository` response mapping (Gson model → UI state), using fixture
    JSON responses for success/error/malformed cases.
- No instrumented/UI tests for v1, matching the scope of Firmium's own initial
  test coverage.

## Open items for the implementation plan

- Exact bundled font files to copy from Firmium's `res/font/` (same set,
  license-compatible — already bundled in Firmium so no new licensing check
  needed).
- Whether Open-Meteo's combined forecast endpoint or two separate calls
  (current vs hourly/daily) fits the repository shape better — an
  implementation-level detail, not a design decision.
