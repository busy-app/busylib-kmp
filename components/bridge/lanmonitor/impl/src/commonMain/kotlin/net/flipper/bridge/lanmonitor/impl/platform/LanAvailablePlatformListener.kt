package net.flipper.bridge.lanmonitor.impl.platform

import kotlinx.coroutines.flow.Flow

interface LanAvailablePlatformListener {
    fun getLanAvailableFlow(): Flow<Boolean>
}