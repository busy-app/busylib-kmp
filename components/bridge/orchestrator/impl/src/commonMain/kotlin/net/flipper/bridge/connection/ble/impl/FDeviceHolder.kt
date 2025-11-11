package net.flipper.bridge.connection.ble.impl

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.connectionbuilder.api.FDeviceConfigToConnection
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.bridge.connection.transport.common.api.FDeviceConnectionConfig
import net.flipper.bridge.connection.transport.common.api.FTransportConnectionStatusListener
import net.flipper.core.busylib.ktx.common.FlipperDispatchers
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.info

// Generics don't work with Anvil/Dagger
@Inject
class FDeviceHolderFactory(
    private val deviceConnectionHelper: FDeviceConfigToConnection
) {
    fun <API : FConnectedDeviceApi> build(
        config: FDeviceConnectionConfig<API>,
        listener: FTransportConnectionStatusListener,
        onConnectError: (Throwable) -> Unit,
        exceptionHandler: CoroutineExceptionHandler
    ): FDeviceHolder<API> {
        return FDeviceHolder(
            config = config,
            listener = listener,
            onConnectError = onConnectError,
            deviceConnectionHelper = deviceConnectionHelper,
            exceptionHandler = exceptionHandler
        )
    }
}

class FDeviceHolder<API : FConnectedDeviceApi>(
    private val config: FDeviceConnectionConfig<API>,
    private val listener: FTransportConnectionStatusListener,
    private val onConnectError: (Throwable) -> Unit,
    private val deviceConnectionHelper: FDeviceConfigToConnection,
    private val exceptionHandler: CoroutineExceptionHandler
) : LogTagProvider {
    override val TAG = "FDeviceHolder-$config"

    private val scope = CoroutineScope(
        FlipperDispatchers.default + exceptionHandler
    )
    private var deviceApi: API? = null
    private val connectJob: Job = scope.launch {
        deviceApi = deviceConnectionHelper.connect(
            scope, config, listener
        ).onFailure(onConnectError).getOrNull()
    }

    suspend fun disconnect() {
        info { "Cancel connect job" }
        connectJob.cancelAndJoin()
        if (deviceApi != null) {
            info { "Find active device api, start disconnect" }
            deviceApi?.disconnect()
        }
        info { "Cancel scope" }
        scope.coroutineContext.job.cancelAndJoin()
        scope.cancel()
    }
}
