# Ventus Features

A complete list of what Ventus can do (Android only).

---

## Weather & Location

- Current conditions: temperature, feels-like, humidity, wind, weather icon/description
- 24-hour hourly forecast strip (temp, precipitation chance, icon)
- 7-day daily forecast list (low/high, icon)
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
