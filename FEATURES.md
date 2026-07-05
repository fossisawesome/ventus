# Ventus Features

A complete list of what Ventus can do (Android only).

---

## Weather & Location

- Current conditions: temperature, feels-like, humidity, wind, UV index, precipitation chance,
  weather icon/description, shown as scannable stat chips
- 24-hour hourly forecast strip (temp, precipitation chance, icon), grouped in a card
- 7-day daily forecast list (low/high, precipitation chance, min–max range bar, icon), grouped in a card
- Sunrise/sunset card with a day-progress arc showing where the sun currently sits between the two
- Air quality card (AQI + category, e.g. "Good"/"Moderate") for the current location
- Tap the location name to open a search sheet (city search + "use current location") without
  reflowing the rest of the screen
- GPS auto-detected location on first launch
- Manual city search as a fallback or override (no location permission required if only using search)
- Auto unit system by locale (metric/imperial), with manual override in Settings
- Pull-to-refresh
- Offline support — last successful forecast is cached and shown (with a "last updated" indicator)
  if a refresh fails or there's no network

---

## Themes & Appearance

- 19 built-in color themes, plus a Ventus flagship default theme
- Custom user themes via `.toml` files
- Import custom themes via a file picker
- Choose the interface font from a curated list (Inter, monospace variants, system fonts, etc.)

---

## Settings

- Theme picker — grid of swatches, tap to apply immediately
- Font picker — list of names, tap to apply immediately
- Units toggle — Auto (locale-based) / Metric / Imperial
- Location controls — "Use current location" (re-triggers GPS) and manual city search override

---

## Account & Security

- No account or login required
- No API key or secrets needed — Open-Meteo's forecast and geocoding endpoints are free and keyless
- Location permission (`ACCESS_COARSE_LOCATION`) requested only when needed for GPS-based weather;
  app remains fully usable via manual search if denied

---

## Not in v1 (by design)

- Multiple saved locations / swiping between cities
- Radar/precipitation map view
- Home screen widget
- Background/periodic auto-refresh
- Wear OS companion
