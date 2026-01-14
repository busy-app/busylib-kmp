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
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionStatus
import net.flipper.bridge.connection.transport.common.api.FTransportConnectionStatusListener
import net.flipper.core.busylib.ktx.common.FlipperDispatchers
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.info

typealias DeviceHolderListener<API, T> = (FDeviceHolder<API>, T) -> Unit

// Generics don't work with Anvil/Dagger
@Inject
class FDeviceHolderFactory(
    private val deviceConnectionHelper: FDeviceConfigToConnection
) {
    fun <API : FConnectedDeviceApi> build(
        config: FDeviceConnectionConfig<API>,
        listener: DeviceHolderListener<API, FInternalTransportConnectionStatus>,
        onConnectError: DeviceHolderListener<API, Throwable>,
        exceptionHandler: DeviceHolderListener<API, Throwable>
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
    val config: FDeviceConnectionConfig<API>,
    private val listener: DeviceHolderListener<API, FInternalTransportConnectionStatus>,
    private val onConnectError: DeviceHolderListener<API, Throwable>,
    private val deviceConnectionHelper: FDeviceConfigToConnection,
    private val exceptionHandler: DeviceHolderListener<API, Throwable>
) : LogTagProvider {
    override val TAG = "FDeviceHolder-$config"

    private val transportConnectionListener = FTransportConnectionStatusListener {
        listener(this, it)
    }

    private val scope = CoroutineScope(
        FlipperDispatchers.default + CoroutineExceptionHandler { _, throwable ->
            exceptionHandler(this, throwable)
        }
    )
    private var deviceApi: API? = null
    private val connectJob: Job = scope.launch {
        deviceApi = deviceConnectionHelper.connect(
            scope, config, transportConnectionListener
        ).onFailure {
            onConnectError(this@FDeviceHolder, it)
        }.getOrNull()
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
