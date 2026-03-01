package com.autorizz.mode

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class Mode { BYOK, PRO }

@Singleton
class ModeManager @Inject constructor(
    private val cellBreakConfig: AutoRizzConfig
) {
    private val _modeFlow = MutableStateFlow(loadMode())
    val modeFlow: StateFlow<Mode> = _modeFlow.asStateFlow()

    val currentMode: Mode get() = _modeFlow.value

    fun switchMode(mode: Mode) {
        cellBreakConfig.userMode = mode.name
        _modeFlow.value = mode
    }

    fun isProMode(): Boolean = currentMode == Mode.PRO
    fun isByokMode(): Boolean = currentMode == Mode.BYOK

    private fun loadMode(): Mode {
        return try {
            Mode.valueOf(cellBreakConfig.userMode)
        } catch (_: Exception) {
            Mode.BYOK
        }
    }
}
