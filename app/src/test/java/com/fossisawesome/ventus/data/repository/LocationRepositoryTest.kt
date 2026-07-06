package com.fossisawesome.ventus.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.fossisawesome.ventus.data.model.Location
import com.fossisawesome.ventus.data.storage.AppPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeDataStore : DataStore<Preferences> {
    private val flow = MutableStateFlow<Preferences>(emptyPreferences())
    override val data = flow
    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
        val updated = transform(flow.value)
        flow.value = updated
        return updated
    }
}

class LocationRepositoryTest {

    @Test
    fun `migrateIfNeeded is a no-op with an empty list when there is nothing to migrate`() = runBlocking {
        val prefs = AppPreferences(FakeDataStore())
        val repo = LocationRepository(prefs)

        assertNull(repo.migrateIfNeeded())
        assertTrue(repo.locationsFlow.first().isEmpty())
    }

    @Test
    fun `migrateIfNeeded converts the old scalar location keys into a single-entry list`() = runBlocking {
        val prefs = AppPreferences(FakeDataStore())
        prefs.setLocation(48.8566, 2.3522, "Paris, France")

        val repo = LocationRepository(prefs)
        val migrated = repo.migrateIfNeeded()

        assertEquals("Paris, France", migrated?.name)
        assertEquals(false, migrated?.isCurrentLocation)
        assertEquals(listOf(migrated), repo.locationsFlow.first())
        assertEquals(migrated?.id, repo.activeLocationIdFlow.first())
        assertNull(prefs.locationName.first()) // old keys cleared post-migration
    }

    @Test
    fun `migrateIfNeeded marks the migrated entry as current-location when the name matches`() = runBlocking {
        val prefs = AppPreferences(FakeDataStore())
        prefs.setLocation(1.0, 1.0, "Current location")

        val migrated = LocationRepository(prefs).migrateIfNeeded()

        assertEquals(AppPreferences.CURRENT_LOCATION_ID, migrated?.id)
        assertEquals(true, migrated?.isCurrentLocation)
    }

    @Test
    fun `migrateIfNeeded is idempotent — does nothing on a second call`() = runBlocking {
        val prefs = AppPreferences(FakeDataStore())
        prefs.setLocation(48.8566, 2.3522, "Paris, France")
        val repo = LocationRepository(prefs)
        repo.migrateIfNeeded()

        assertNull(repo.migrateIfNeeded())
        assertEquals(1, repo.locationsFlow.first().size)
    }

    @Test
    fun `addLocation appends and sets it active`() = runBlocking {
        val prefs = AppPreferences(FakeDataStore())
        val repo = LocationRepository(prefs)
        val loc = Location("geo:1", 1.0, 1.0, "A", null)

        val result = repo.addLocation(loc)

        assertEquals(LocationRepository.AddResult.Added, result)
        assertEquals(listOf(loc), repo.locationsFlow.first())
        assertEquals("geo:1", repo.activeLocationIdFlow.first())
    }

    @Test
    fun `addLocation reports AlreadyExists and just re-activates instead of duplicating`() = runBlocking {
        val prefs = AppPreferences(FakeDataStore())
        val repo = LocationRepository(prefs)
        val loc = Location("geo:1", 1.0, 1.0, "A", null)
        repo.addLocation(loc)
        repo.setActiveLocationId("something-else")

        val result = repo.addLocation(loc)

        assertEquals(LocationRepository.AddResult.AlreadyExists, result)
        assertEquals(1, repo.locationsFlow.first().size)
        assertEquals("geo:1", repo.activeLocationIdFlow.first())
    }

    @Test
    fun `addLocation reports CapReached at MAX_SAVED_LOCATIONS`() = runBlocking {
        val prefs = AppPreferences(FakeDataStore())
        val repo = LocationRepository(prefs)
        repeat(AppPreferences.MAX_SAVED_LOCATIONS) { i -> repo.addLocation(Location("geo:$i", i.toDouble(), i.toDouble(), "City $i", null)) }

        val result = repo.addLocation(Location("geo:overflow", 99.0, 99.0, "Overflow", null))

        assertEquals(LocationRepository.AddResult.CapReached, result)
        assertEquals(AppPreferences.MAX_SAVED_LOCATIONS, repo.locationsFlow.first().size)
    }

    @Test
    fun `removeLocation drops the entry and re-activates a fallback when the active one is removed`() = runBlocking {
        val prefs = AppPreferences(FakeDataStore())
        val repo = LocationRepository(prefs)
        repo.addLocation(Location("geo:1", 1.0, 1.0, "A", null))
        repo.addLocation(Location("geo:2", 2.0, 2.0, "B", null))
        repo.setActiveLocationId("geo:2")

        repo.removeLocation("geo:2")

        assertEquals(listOf(Location("geo:1", 1.0, 1.0, "A", null)), repo.locationsFlow.first())
        assertEquals("geo:1", repo.activeLocationIdFlow.first())
    }

    @Test
    fun `reorder applies the given id order`() = runBlocking {
        val prefs = AppPreferences(FakeDataStore())
        val repo = LocationRepository(prefs)
        repo.addLocation(Location("geo:1", 1.0, 1.0, "A", null))
        repo.addLocation(Location("geo:2", 2.0, 2.0, "B", null))

        repo.reorder(listOf("geo:2", "geo:1"))

        assertEquals(listOf("geo:2", "geo:1"), repo.locationsFlow.first().map { it.id })
    }

    @Test
    fun `upsertCurrentLocationCoords updates only the current-location entry`() = runBlocking {
        val prefs = AppPreferences(FakeDataStore())
        val repo = LocationRepository(prefs)
        repo.addLocation(Location(AppPreferences.CURRENT_LOCATION_ID, 1.0, 1.0, "Current location", null, isCurrentLocation = true))
        repo.addLocation(Location("geo:1", 5.0, 5.0, "A", null))

        repo.upsertCurrentLocationCoords(9.0, 9.0)

        val locations = repo.locationsFlow.first()
        assertEquals(9.0, locations.first { it.isCurrentLocation }.lat, 0.0001)
        assertEquals(5.0, locations.first { it.id == "geo:1" }.lat, 0.0001) // untouched
    }
}
