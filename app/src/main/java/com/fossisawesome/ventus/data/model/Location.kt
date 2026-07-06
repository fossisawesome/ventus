package com.fossisawesome.ventus.data.model

// A user-saved location — distinct from GeocodingResult, which is a pure, ephemeral search-result
// DTO shaped after Open-Meteo's geocoding response. Location is the persisted domain type: it has
// a stable String id (so both geocoding-sourced entries and the single hardcoded GPS entry can
// share an id space) and isCurrentLocation, which tells WeatherViewModel to re-resolve lat/lon via
// LocationSource on every refresh instead of trusting the last-persisted coordinates.
data class Location(
    val id: String,
    val lat: Double,
    val lon: Double,
    val name: String,
    val country: String?,
    val isCurrentLocation: Boolean = false,
)

// Prefixed with "geo:" so a geocoding-sourced id can never collide with the reserved
// AppPreferences.CURRENT_LOCATION_ID sentinel used for the one GPS-tracked entry.
fun GeocodingResult.toSavedLocation(): Location = Location(
    id = "geo:$id",
    lat = latitude,
    lon = longitude,
    name = listOfNotNull(name, country).joinToString(", "),
    country = country,
    isCurrentLocation = false,
)

// Mirrors the "active id stored as a single scalar key, resolved via lookup-with-fallback" idiom
// already used for themes (see ui/theme/AppTheme.kt's themeById()) — if the persisted active id no
// longer matches any saved location (e.g. it was just removed), fall back to the first entry
// rather than leaving the UI pointed at nothing.
fun resolveActiveLocationId(locations: List<Location>, requestedId: String?): String? =
    locations.find { it.id == requestedId }?.id ?: locations.firstOrNull()?.id
