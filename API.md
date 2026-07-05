# Open-Meteo API Reference

Reference for the Open-Meteo API surface Ventus uses. Both endpoints are free and require no API
key or auth token.

---

## Forecast Endpoint

`GET https://api.open-meteo.com/v1/forecast`

Called from `data/api/WeatherApi.kt::OpenMeteoWeatherApi.fetchForecast(lat, lon)`.

### Request parameters (as sent by Ventus)

| Parameter | Value | Notes |
|---|---|---|
| `latitude` / `longitude` | from `LocationSource` or geocoding selection | |
| `current` | `temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m` | |
| `hourly` | `temperature_2m,precipitation_probability,weather_code` | 24h strip on `MainScreen` |
| `daily` | `weather_code,temperature_2m_max,temperature_2m_min` | 7-day list |
| `temperature_unit` | `celsius` | always — see below |
| `wind_speed_unit` | `kmh` | always |
| `precipitation_unit` | `mm` | always |
| `timezone` | `auto` | resolves to the queried location's local timezone |
| `forecast_days` | `7` | |

**Ventus always requests metric units from the API itself**, regardless of the user's display unit
setting. `WeatherSnapshot` (the domain model) stores SI values only; display-time conversion
(`celsiusToFahrenheit`, `kmhToMph`, `mmToIn` in `data/UnitConversions.kt`) is applied in the UI
layer. Requesting Open-Meteo's own unit conversion here would double-convert once the UI also
converts for imperial display — see the comment on `fetchForecast()`.

### Response shape

Parsed into `OpenMeteoForecastResponse` (`data/model/WeatherModels.kt`), with nested `current`,
`hourly`, `daily` blocks. `hourly`/`daily` blocks are parallel arrays (`time[i]`, `temperature_2m[i]`,
etc.) — Open-Meteo's convention, not Ventus-specific. `mapForecastResponse()` in `WeatherApi.kt`
zips these into `HourlyPoint`/`DailyPoint` domain lists on `WeatherSnapshot`.

`hourly.time[i]` / `daily.time[i]` are ISO-local-time strings (e.g. `"2026-07-05T14:00"` /
`"2026-07-05"`). `isoLocalTimeToEpochSeconds()` parses these as UTC for relative ordering/display
within a single location — see the comment in `WeatherApi.kt` for why this is sufficient (single
active location, no cross-timezone comparison needed).

### Weather codes

`current.weather_code` / `hourly.weather_code[i]` / `daily.weather_code[i]` are WMO weather
interpretation codes (Open-Meteo's documented code table, e.g. `0` = clear sky, `61` = slight rain,
`95` = thunderstorm). Mapped to a description + icon in `data/UnitConversions.kt` (weather-code
helper functions) — the single source of truth for code → UI mapping.

### Error handling

`OpenMeteoWeatherApi.fetchForecast()` throws (`error(...)`, an `IllegalStateException`) on a
non-2xx response or empty body. `WeatherRepository.refresh()` catches this and falls back to the
cached `WeatherSnapshot` if present (`WeatherUiState.Stale`), or surfaces `WeatherUiState.Error` if
no cache exists.

---

## Geocoding Endpoint

`GET https://geocoding-api.open-meteo.com/v1/search`

Called from `data/api/GeocodingApi.kt::OpenMeteoGeocodingApi.search(query)`, used by the manual
city search on `MainScreen`'s empty state and `SettingsScreen`'s location override.

### Request parameters

| Parameter | Value |
|---|---|
| `name` | user's search query |
| `count` | `10` |
| `language` | `en` |
| `format` | `json` |

Blank query short-circuits to an empty list without a network call.

### Response shape

Parsed into `GeocodingSearchResponse` → `results: List<GeocodingResult>?` (nullable — Open-Meteo
omits the field entirely on zero matches, mapped to `emptyList()`). Each `GeocodingResult` carries
name, admin area, country, `latitude`/`longitude` — selecting one in the UI calls
`WeatherViewModel.selectLocation()`, which persists it via `AppPreferences.setLocation()` and
triggers a fresh forecast fetch.

---

## Caveats

- **No API key, no rate-limit handling implemented** — Open-Meteo's free tier is generous enough
  for a single-location personal-use client; Ventus does not implement backoff/retry.
- **Single request per refresh** — the combined forecast endpoint (current + hourly + daily in one
  call) is used rather than separate calls, to minimize round trips for pull-to-refresh.
- **No caching or debouncing of geocoding results** — every search keystroke-triggered query hits
  the network directly; `WeatherViewModel.search()` cancels any in-flight search job before
  starting a new one so a stale response can't overwrite a later query's results, but there's no
  time-based debounce.
