package net.flipper.bsb.watchers.provisioning.fakes

import kotlinx.coroutines.flow.MutableStateFlow
import net.flipper.bridge.connection.orchestrator.api.FDeviceOrchestrator
import net.flipper.bridge.connection.orchestrator.api.model.FDeviceConnectStatus
import net.flipper.busylib.core.wrapper.WrappedStateFlow
import net.flipper.busylib.core.wrapper.wrap

internal class FakeOrchestrator(
    private val stateFlow: MutableStateFlow<FDeviceConnectStatus>
) : FDeviceOrchestrator {
    override fun getState(): WrappedStateFlow<FDeviceConnectStatus> = stateFlow.wrap()
}
