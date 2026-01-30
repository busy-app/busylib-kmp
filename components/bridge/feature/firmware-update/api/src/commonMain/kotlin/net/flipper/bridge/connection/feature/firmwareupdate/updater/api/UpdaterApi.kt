package net.flipper.bridge.connection.feature.firmwareupdate.updater.api

import kotlinx.coroutines.flow.StateFlow
import net.flipper.bridge.connection.feature.firmwareupdate.updater.model.FwUpdateState
import net.flipper.busylib.core.wrapper.WrappedFlow
import net.flipper.busylib.core.wrapper.WrappedStateFlow

interface UpdaterApi {
    val state: WrappedStateFlow<FwUpdateState>
}
