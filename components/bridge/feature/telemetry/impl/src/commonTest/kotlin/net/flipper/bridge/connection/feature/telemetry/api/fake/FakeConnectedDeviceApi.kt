package net.flipper.bridge.connection.feature.telemetry.api.fake

import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.bridge.connection.transport.common.api.FDeviceConnectionConfig

internal class FakeConnectedDeviceApi : FConnectedDeviceApi {
    override val deviceName: String = "test-device"

    override suspend fun tryUpdateConnectionConfig(
        config: FDeviceConnectionConfig<*>
    ): Result<Unit> = Result.success(Unit)

    override suspend fun disconnect() = Unit
}
