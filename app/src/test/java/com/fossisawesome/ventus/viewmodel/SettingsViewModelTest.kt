package com.fossisawesome.ventus.viewmodel

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.fossisawesome.ventus.data.model.Location
import com.fossisawesome.ventus.data.repository.LocationRepository
import com.fossisawesome.ventus.data.storage.AppPreferences
import com.fossisawesome.ventus.ui.theme.ALL_THEMES
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class SettingsFakeDataStore : DataStore<Preferences> {
    private val flow = MutableStateFlow<Preferences>(emptyPreferences())
    override val data = flow
    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
        val updated = transform(flow.value)
        flow.value = updated
        return updated
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() { Dispatchers.setMain(testDispatcher) }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    private fun buildVm(prefs: AppPreferences): SettingsViewModel =
        SettingsViewModel(prefs, LocationRepository(prefs), loadThemes = { ALL_THEMES }, importTheme = { Result.success(Unit) }, deleteTheme = {})

    @Test
    fun `selectTheme persists the new theme id`() = runTest {
        val prefs = AppPreferences(SettingsFakeDataStore())
        val vm = buildVm(prefs)

        vm.selectTheme("dracula")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("dracula", prefs.themeId.first())
    }

    @Test
    fun `selectFont persists the new font name`() = runTest {
        val prefs = AppPreferences(SettingsFakeDataStore())
        val vm = buildVm(prefs)

        vm.selectFont("Hack")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Hack", prefs.fontFamily.first())
    }

    @Test
    fun `selectUnitsMode persists the new mode`() = runTest {
        val prefs = AppPreferences(SettingsFakeDataStore())
        val vm = buildVm(prefs)

        vm.selectUnitsMode("imperial")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("imperial", prefs.unitsMode.first())
    }

    @Test
    fun `weatherProvider defaults to open-meteo`() = runTest {
        val prefs = AppPreferences(SettingsFakeDataStore())
        val vm = buildVm(prefs)

        assertEquals("open-meteo", vm.weatherProvider.value)
    }

    @Test
    fun `selectWeatherProvider persists the choice`() = runTest {
        val prefs = AppPreferences(SettingsFakeDataStore())
        val vm = buildVm(prefs)

        vm.selectWeatherProvider("nws")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("nws", prefs.weatherProvider.first())
    }

    @Test
    fun `isNwsAvailable is false when there is no active location`() = runTest {
        val prefs = AppPreferences(SettingsFakeDataStore())
        val vm = buildVm(prefs)

        assertEquals(false, vm.isNwsAvailable.value)
    }

    @Test
    fun `isNwsAvailable is true when the active saved location is in the US`() = runTest {
        val prefs = AppPreferences(SettingsFakeDataStore())
        val locationRepo = LocationRepository(prefs)
        locationRepo.addLocation(Location("geo:1", 40.7128, -74.0060, "New York", null))
        val vm = SettingsViewModel(prefs, locationRepo, loadThemes = { ALL_THEMES }, importTheme = { Result.success(Unit) }, deleteTheme = {})
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(true, vm.isNwsAvailable.value)
    }

    @Test
    fun `isNwsAvailable is false when the active saved location is outside the US`() = runTest {
        val prefs = AppPreferences(SettingsFakeDataStore())
        val locationRepo = LocationRepository(prefs)
        locationRepo.addLocation(Location("geo:1", 51.5072, -0.1276, "London", "UK"))
        val vm = SettingsViewModel(prefs, locationRepo, loadThemes = { ALL_THEMES }, importTheme = { Result.success(Unit) }, deleteTheme = {})
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(false, vm.isNwsAvailable.value)
    }

    @Test
    fun `availableThemes reflects loadThemes at construction time`() = runTest {
        val prefs = AppPreferences(SettingsFakeDataStore())
        val custom = ALL_THEMES.first().copy(id = "custom", name = "Custom", isImported = true, sourceFile = "custom.toml")
        val vm = SettingsViewModel(prefs, LocationRepository(prefs), loadThemes = { ALL_THEMES + custom }, importTheme = { Result.success(Unit) }, deleteTheme = {})

        assertTrue(vm.availableThemes.value.any { it.id == "custom" })
    }

    @Test
    fun `importThemeFile refreshes availableThemes on success`() = runTest {
        val prefs = AppPreferences(SettingsFakeDataStore())
        var themesAfterImport = ALL_THEMES
        val vm = SettingsViewModel(
            prefs,
            LocationRepository(prefs),
            loadThemes = { themesAfterImport },
            importTheme = { _ ->
                themesAfterImport = ALL_THEMES + ALL_THEMES.first().copy(id = "imported", name = "Imported", isImported = true, sourceFile = "imported.toml")
                Result.success(Unit)
            },
            deleteTheme = {},
        )

        val result = vm.importThemeFile("content://fake")

        assertTrue(result.isSuccess)
        assertTrue(vm.availableThemes.value.any { it.id == "imported" })
    }

    @Test
    fun `deleteThemeFile removes the theme and refreshes availableThemes`() = runTest {
        val prefs = AppPreferences(SettingsFakeDataStore())
        val imported = ALL_THEMES.first().copy(id = "imported", name = "Imported", isImported = true, sourceFile = "imported.toml")
        var themes = ALL_THEMES + imported
        var deletedFile: String? = null
        val vm = SettingsViewModel(
            prefs,
            LocationRepository(prefs),
            loadThemes = { themes },
            importTheme = { Result.success(Unit) },
            deleteTheme = { file -> deletedFile = file; themes = ALL_THEMES },
        )

        vm.deleteThemeFile(imported)

        assertEquals("imported.toml", deletedFile)
        assertTrue(vm.availableThemes.value.none { it.id == "imported" })
    }
}
