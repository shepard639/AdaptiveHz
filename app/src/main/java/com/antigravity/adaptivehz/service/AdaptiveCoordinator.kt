package com.antigravity.adaptivehz.service

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Thread-safe coordinator for reactive communication between services and UI.
 * Eliminates static context references and tight coupling.
 */
object AdaptiveCoordinator {

    private val _isServiceActive = MutableStateFlow(false)
    val isServiceActive: StateFlow<Boolean> = _isServiceActive.asStateFlow()

    private val _adaptiveState = MutableStateFlow(AdaptiveState.ACTIVE_TOUCH)
    val adaptiveState: StateFlow<AdaptiveState> = _adaptiveState.asStateFlow()

    private val _activePackageName = MutableStateFlow("")
    val activePackageName: StateFlow<String> = _activePackageName.asStateFlow()

    private val _userInteractionEvents = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val userInteractionEvents: SharedFlow<Unit> = _userInteractionEvents.asSharedFlow()

    fun setServiceActive(active: Boolean) {
        _isServiceActive.value = active
    }

    fun setAdaptiveState(state: AdaptiveState) {
        _adaptiveState.value = state
    }

    fun setActivePackageName(packageName: String) {
        _activePackageName.value = packageName
    }

    fun notifyUserInteraction() {
        _userInteractionEvents.tryEmit(Unit)
    }
}
