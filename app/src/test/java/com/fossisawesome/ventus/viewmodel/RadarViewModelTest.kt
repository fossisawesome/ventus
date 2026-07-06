package com.fossisawesome.ventus.viewmodel

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.fossisawesome.ventus.data.api.RainViewerApi
import com.fossisawesome.ventus.data.model.RadarFrame
import com.fossisawesome.ventus.data.repository.LocationRepository
import com.fossisawesome.ventus.data.storage.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class RadarFakeDataStore : DataStore<Preferences> {
    private val flow = MutableStateFlow<Preferences>(emptyPreferences())
    override val data = flow
    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
        val updated = transform(flow.value)
        flow.value = updated
        return updated
    }
}

private val sampleFrames = listOf(
    RadarFrame(timeEpochSeconds = 1L, path = "/a", host = "https://h"),
    RadarFrame(timeEpochSeconds = 2L, path = "/b", host = "https://h"),
    RadarFrame(timeEpochSeconds = 3L, path = "/c", host = "https://h"),
)

private class FakeRainViewerApi(
    private val frames: List<RadarFrame> = sampleFrames,
    private val shouldFail: Boolean = false,
) : RainViewerApi {
    override suspend fun fetchFrames(): List<RadarFrame> {
        if (shouldFail) error("network down")
        return frames
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class RadarViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() { Dispatchers.setMain(testDispatcher) }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    private fun buildVm(rainViewerApi: RainViewerApi = FakeRainViewerApi()): RadarViewModel {
        val prefs = AppPreferences(RadarFakeDataStore())
        return RadarViewModel(rainViewerApi, LocationRepository(prefs))
    }

    @Test
    fun `loadFrames populates frames, defaults to the last frame, and starts playing`() = runTest {
        val vm = buildVm()

        vm.loadFrames()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(sampleFrames, vm.frames.value)
        assertEquals(2, vm.currentFrameIndex.value) // last index
        assertTrue(vm.isPlaying.value)
    }

    @Test
    fun `loadFrames failure leaves frames empty and not playing`() = runTest {
        val vm = buildVm(FakeRainViewerApi(shouldFail = true))

        vm.loadFrames()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.frames.value.isEmpty())
        assertEquals(false, vm.isPlaying.value)
    }

    @Test
    fun `togglePlayback flips isPlaying`() = runTest {
        val vm = buildVm()
        vm.loadFrames()
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.isPlaying.value)

        vm.togglePlayback()
        assertEquals(false, vm.isPlaying.value)

        vm.togglePlayback()
        assertTrue(vm.isPlaying.value)
    }

    @Test
    fun `advanceFrame wraps back to zero after the last frame`() = runTest {
        val vm = buildVm()
        vm.loadFrames()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(2, vm.currentFrameIndex.value)

        vm.advanceFrame()

        assertEquals(0, vm.currentFrameIndex.value)
    }

    @Test
    fun `advanceFrame is a no-op when there are no frames`() = runTest {
        val vm = buildVm(FakeRainViewerApi(shouldFail = true))
        vm.loadFrames()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.advanceFrame()

        assertEquals(0, vm.currentFrameIndex.value)
    }
}
