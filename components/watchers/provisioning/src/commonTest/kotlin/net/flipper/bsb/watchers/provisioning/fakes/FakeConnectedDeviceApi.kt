package net.flipper.bsb.watchers.provisioning.fakes

import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.bridge.connection.transport.common.api.FDeviceConnectionConfig

internal class FakeConnectedDeviceApi : FConnectedDeviceApi {
    override val deviceName = "Test Device"
    override suspend fun tryUpdateConnectionConfig(
        config: FDeviceConnectionConfig<*>
    ): Result<Unit> = Result.failure(NotImplementedError())

    override suspend fun disconnect() = Unit
}
