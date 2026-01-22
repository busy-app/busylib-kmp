package net.flipper.bridge.connection.feature.firmwareupdate.updater.api

import kotlinx.coroutines.flow.StateFlow
import net.flipper.bridge.connection.feature.firmwareupdate.updater.model.FwUpdateState

interface UpdaterApi {
    val state: StateFlow<FwUpdateState>
}
