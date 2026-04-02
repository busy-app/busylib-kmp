package net.flipper.bridge.connection.device.bsb.api

import kotlinx.coroutines.CoroutineScope
import net.flipper.bridge.connection.device.common.api.FDeviceApi
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import uk.kulikov.metro.assisted.AssistedKey

interface FBSBDeviceApi : FDeviceApi {

    fun interface Factory {
        operator fun invoke(
            @AssistedKey("scope") scope: CoroutineScope,
            @AssistedKey("connectedDevice") connectedDevice: FConnectedDeviceApi
        ): FBSBDeviceApi
    }
}
