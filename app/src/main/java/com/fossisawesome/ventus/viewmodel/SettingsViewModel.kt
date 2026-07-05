package com.fossisawesome.ventus.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fossisawesome.ventus.data.storage.AppPreferences
import com.fossisawesome.ventus.ui.theme.AppTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// importTheme/deleteTheme take a plain String (the picked file's URI, stringified) rather than
// android.net.Uri directly — Uri is unusable in plain JUnit tests without Robolectric (it's a
// platform type whose real methods throw outside an Android runtime), so keeping this class
// framework-agnostic makes it fully unit-testable. MainActivity converts to/from Uri at the edge.
class SettingsViewModel(
    private val prefs: AppPreferences,
    private val loadThemes: () -> List<AppTheme>,
    private val importTheme: (String) -> Result<Unit>,
    private val deleteTheme: (String) -> Unit,
) : ViewModel() {

    val themeId: StateFlow<String> = prefs.themeId.stateIn(viewModelScope, SharingStarted.Eagerly, "ventus")
    val fontFamily: StateFlow<String> = prefs.fontFamily.stateIn(viewModelScope, SharingStarted.Eagerly, "Liberation Mono")
    val unitsMode: StateFlow<String> = prefs.unitsMode.stateIn(viewModelScope, SharingStarted.Eagerly, "auto")

    private val _availableThemes = MutableStateFlow(loadThemes())
    val availableThemes: StateFlow<List<AppTheme>> = _availableThemes

    fun selectTheme(id: String) {
        viewModelScope.launch { prefs.setThemeId(id) }
    }

    fun selectFont(name: String) {
        viewModelScope.launch { prefs.setFontFamily(name) }
    }

    fun selectUnitsMode(mode: String) {
        viewModelScope.launch { prefs.setUnitsMode(mode) }
    }

    fun importThemeFile(uriString: String): Result<Unit> {
        val result = importTheme(uriString)
        if (result.isSuccess) _availableThemes.value = loadThemes()
        return result
    }

    fun deleteThemeFile(theme: AppTheme) {
        val file = theme.sourceFile ?: return
        deleteTheme(file)
        _availableThemes.value = loadThemes()
    }
}
