package net.flipper.bridge.connection.ble.impl

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.job
import net.flipper.bridge.connection.config.api.model.BUSYBar
import net.flipper.bridge.connection.connectionbuilder.api.FDeviceConfigToConnection
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.bridge.connection.transport.common.api.FDeviceConnectionConfig
import net.flipper.bridge.connection.transport.common.api.FInternalDisconnectedReason
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionStatus
import net.flipper.bridge.connection.transport.common.api.FTransportConnectionStatusListener
import net.flipper.bridge.connection.transport.common.api.FailedPairingConnectException
import net.flipper.core.busylib.ktx.common.FlipperDispatchers
import net.flipper.core.busylib.ktx.common.runSuspendCatching
import net.flipper.core.busylib.ktx.common.transform
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.error
import net.flipper.core.busylib.log.info

// Generics don't work with Anvil/Dagger
@Inject
class FDeviceHolderFactory(
    private val deviceConnectionHelper: FDeviceConfigToConnection
) {
    fun <API : FConnectedDeviceApi> build(
        uniqueId: String,
        config: FDeviceConnectionConfig<API>,
        listener: FTransportListenerImpl
    ): FDeviceHolder<API> {
        return FDeviceHolder(
            uniqueId = uniqueId,
            config = config,
            listener = listener,
            deviceConnectionHelper = deviceConnectionHelper
        )
    }
}

class FDeviceHolder<API : FConnectedDeviceApi>(
    val uniqueId: String,
    private val config: FDeviceConnectionConfig<API>,
    private val listener: FTransportListenerImpl,
    private val deviceConnectionHelper: FDeviceConfigToConnection
) : LogTagProvider {
    override val TAG = "FDeviceHolder-$config"

    private val transportConnectionListener = FTransportConnectionStatusListener { status ->
        listener.onStatusUpdate(this, status)
    }

    private val scope = CoroutineScope(
        FlipperDispatchers.default + CoroutineExceptionHandler { _, throwable ->
            error(throwable) { "Exception in coroutine" }
            listener.onErrorDuringConnect(this, throwable)
        }
    )

    private val deviceApi: Deferred<API> = scope.async {
        runSuspendCatching {
            deviceConnectionHelper.connect(
                scope = scope,
                config = config,
                listener = transportConnectionListener
            )
        }.transform { it }
            .onFailure { t ->
                error(t) { "Exception in init" }
                listener.onErrorDuringConnect(this@FDeviceHolder, t)
                scope.cancel(CancellationException("Connect failed", t))
            }.getOrThrow()
    }

    @Suppress("RunCatchingInSuspendRule")
    suspend fun tryToUpdateConnectionConfig(
        bbConfig: BUSYBar,
        config: FDeviceConnectionConfig<*>
    ): Result<Unit> {
        return runCatching { // By design to handle cancellation exception
            deviceApi.getCompleted()
        }.transform { deviceApi ->
            deviceApi.tryUpdateConnectionConfig(config)
        }.onSuccess {
            listener.updateConfig(bbConfig)
        }
    }

    @Suppress("RunCatchingInSuspendRule")
    suspend fun disconnect() {
        info { "Find active device api, start disconnect" }
        deviceApi.cancelAndJoin()
        runCatching { deviceApi.getCompleted() } // By design to handle cancellation exception
            .getOrNull()
            ?.disconnect()
        info { "Cancel scope" }
        scope.coroutineContext.job.cancelAndJoin()
        scope.cancel()
    }

    init {
        scope.coroutineContext.job.invokeOnCompletion { t ->
            when (t) {
                null, is CancellationException -> {
                    listener.onStatusUpdate(
                        this,
                        FInternalTransportConnectionStatus.Disconnected(
                            FInternalDisconnectedReason.OTHER
                        )
                    )
                }

                is FailedPairingConnectException -> {
                    listener.onStatusUpdate(
                        this,
                        FInternalTransportConnectionStatus.Disconnected(
                            FInternalDisconnectedReason.REQUIRES_REPAIRING
                        )
                    )
                }

                else -> {
                    error(t) {
                        "#init catch error during invokeOnCompletion. " +
                            "Status update will be handled inside exception handler"
                    }
                    listener.onStatusUpdate(
                        this,
                        FInternalTransportConnectionStatus.Disconnected(
                            FInternalDisconnectedReason.OTHER
                        )
                    )
                }
            }
        }
    }
}
