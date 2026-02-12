package net.flipper.bridge.connection.ble.impl

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.job
import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.connectionbuilder.api.FDeviceConfigToConnection
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.bridge.connection.transport.common.api.FDeviceConnectionConfig
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionStatus
import net.flipper.bridge.connection.transport.common.api.FTransportConnectionStatusListener
import net.flipper.core.busylib.ktx.common.FlipperDispatchers
import net.flipper.core.busylib.ktx.common.transform
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.info

typealias DeviceHolderListener<API, T> = (FDeviceHolder<API>, T) -> Unit

// Generics don't work with Anvil/Dagger
@Inject
class FDeviceHolderFactory(
    private val deviceConnectionHelper: FDeviceConfigToConnection
) {
    fun <API : FConnectedDeviceApi> build(
        uniqueId: String,
        config: FDeviceConnectionConfig<API>,
        listener: DeviceHolderListener<API, FInternalTransportConnectionStatus>,
        onConnectError: DeviceHolderListener<API, Throwable>,
        exceptionHandler: DeviceHolderListener<API, Throwable>
    ): FDeviceHolder<API> {
        return FDeviceHolder(
            uniqueId = uniqueId,
            config = config,
            listener = listener,
            onConnectError = onConnectError,
            deviceConnectionHelper = deviceConnectionHelper,
            exceptionHandler = exceptionHandler
        )
    }
}

class FDeviceHolder<API : FConnectedDeviceApi>(
    val uniqueId: String,
    private val config: FDeviceConnectionConfig<API>,
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
    private var deviceApi: Deferred<API> = scope.async {
        deviceConnectionHelper.connect(
            scope,
            config,
            transportConnectionListener
        ).onFailure {
            onConnectError(this@FDeviceHolder, it)
        }.getOrThrow()
    }

    suspend fun tryToUpdateConnectionConfig(
        config: FDeviceConnectionConfig<*>
    ): Result<Unit> {
        return runCatching {
            deviceApi.getCompleted()
        }.transform {
            it.tryUpdateConnectionConfig(config)
        }
    }

    suspend fun disconnect() {
        info { "Find active device api, start disconnect" }
        deviceApi.cancelAndJoin()
        runCatching {
            deviceApi.getCompleted()
        }.getOrNull()?.disconnect()
        info { "Cancel scope" }
        scope.coroutineContext.job.cancelAndJoin()
        scope.cancel()
    }
}
