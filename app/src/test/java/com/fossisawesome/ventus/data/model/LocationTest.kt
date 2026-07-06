package com.fossisawesome.ventus.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LocationTest {

    @Test
    fun `toSavedLocation prefixes the geocoding id and joins name plus country`() {
        val result = GeocodingResult(id = 2988507, name = "Paris", latitude = 48.8566, longitude = 2.3522, country = "France", admin1 = "Ile-de-France")

        val location = result.toSavedLocation()

        assertEquals("geo:2988507", location.id)
        assertEquals("Paris, France", location.name)
        assertEquals(48.8566, location.lat, 0.0001)
        assertEquals(2.3522, location.lon, 0.0001)
        assertEquals("France", location.country)
        assertEquals(false, location.isCurrentLocation)
    }

    @Test
    fun `toSavedLocation falls back to name only when country is null`() {
        val result = GeocodingResult(id = 1, name = "Nullsville", latitude = 0.0, longitude = 0.0, country = null, admin1 = null)

        assertEquals("Nullsville", result.toSavedLocation().name)
    }

    @Test
    fun `resolveActiveLocationId keeps the requested id when it still exists`() {
        val locations = listOf(
            Location("a", 1.0, 1.0, "A", null),
            Location("b", 2.0, 2.0, "B", null),
        )
        assertEquals("b", resolveActiveLocationId(locations, "b"))
    }

    @Test
    fun `resolveActiveLocationId falls back to the first location when the requested id is gone`() {
        val locations = listOf(Location("a", 1.0, 1.0, "A", null))
        assertEquals("a", resolveActiveLocationId(locations, "deleted-id"))
    }

    @Test
    fun `resolveActiveLocationId returns null for an empty list`() {
        assertNull(resolveActiveLocationId(emptyList(), "anything"))
    }
}
