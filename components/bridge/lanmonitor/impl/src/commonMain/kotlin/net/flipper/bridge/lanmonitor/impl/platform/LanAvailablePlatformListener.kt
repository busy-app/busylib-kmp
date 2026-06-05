package net.flipper.bridge.lanmonitor.impl.platform

import kotlinx.coroutines.flow.StateFlow

interface LanAvailablePlatformListener {
    fun getLanAvailableFlow(): StateFlow<Boolean>
}