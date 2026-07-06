package com.fossisawesome.ventus.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fossisawesome.ventus.data.api.RainViewerApi
import com.fossisawesome.ventus.data.model.Location
import com.fossisawesome.ventus.data.model.RadarFrame
import com.fossisawesome.ventus.data.repository.LocationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RadarViewModel(
    private val rainViewerApi: RainViewerApi,
    private val locationRepository: LocationRepository,
) : ViewModel() {

    private val _frames = MutableStateFlow<List<RadarFrame>>(emptyList())
    val frames: StateFlow<List<RadarFrame>> = _frames.asStateFlow()

    private val _currentFrameIndex = MutableStateFlow(0)
    val currentFrameIndex: StateFlow<Int> = _currentFrameIndex.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    val activeLocation: StateFlow<Location?> = combine(
        locationRepository.locationsFlow,
        locationRepository.activeLocationIdFlow,
    ) { locations, activeId -> locations.find { it.id == activeId } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // Fetch failure leaves frames empty — MapScreen still renders the base map layer with no
    // radar overlay, no error UI (a missing radar layer is a bonus feature failing, not the
    // whole screen).
    fun loadFrames() {
        viewModelScope.launch {
            val fetched = try {
                rainViewerApi.fetchFrames()
            } catch (_: Exception) {
                emptyList()
            }
            _frames.value = fetched
            _currentFrameIndex.value = (fetched.size - 1).coerceAtLeast(0)
            _isPlaying.value = fetched.isNotEmpty()
        }
    }

    fun togglePlayback() {
        _isPlaying.value = !_isPlaying.value
    }

    // Called by MapScreen's playback LaunchedEffect timer, not on a ViewModel-owned clock — the
    // timer loop lives in the composable, matching this codebase's existing convention of keeping
    // animation timing in the UI layer (e.g. SunArc's remember-based animation).
    fun advanceFrame() {
        val size = _frames.value.size
        if (size == 0) return
        _currentFrameIndex.value = (_currentFrameIndex.value + 1) % size
    }
}
