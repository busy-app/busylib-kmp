package net.flipper.bsb.watchers.provisioning.fakes

import kotlinx.coroutines.flow.Flow
import net.flipper.bridge.connection.feature.hardwareid.api.FHardwareIdFeatureApi
import net.flipper.busylib.core.wrapper.WrappedFlow
import net.flipper.busylib.core.wrapper.wrap

internal class FakeHardwareIdFeatureApi(
    private val hardwareIdFlow: Flow<String?>
) : FHardwareIdFeatureApi {
    override fun getHardwareIdFlow(): WrappedFlow<String?> = hardwareIdFlow.wrap()
}
