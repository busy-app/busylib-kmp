package net.flipper.bridge.connection.transport.mock.impl.api.http.serial

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.transport.ble.api.FBleDeviceSerialConfig
import net.flipper.bridge.connection.transport.ble.api.FSerialBleApi
import net.flipper.busylib.core.wrapper.WrappedStateFlow
import net.flipper.core.busylib.log.LogTagProvider
import no.nordicsemi.kotlin.ble.client.RemoteService

@Inject
class SerialApiFactory(
    private val unsafeApiImplFactory: FSerialUnsafeApiImpl.Factory,
) : LogTagProvider {
    override val TAG = "SerialApiCombinedFactory"

    fun build(
        config: FBleDeviceSerialConfig,
        services: WrappedStateFlow<List<RemoteService>?>,
        scope: CoroutineScope
    ): FSerialBleApi {
        val serialService = services.map { services ->
            services?.find { it.uuid == config.serialServiceUuid }
        }
        val rxCharacteristic = serialService.map { service ->
            service?.characteristics?.find { it.uuid == config.rxServiceCharUuid }
        }
        val txCharacteristic = serialService.map { service ->
            service?.characteristics?.find { it.uuid == config.txServiceCharUuid }
        }
        val unsafeApi = unsafeApiImplFactory(
            rxCharacteristic = rxCharacteristic,
            txCharacteristic = txCharacteristic,
            scope = scope
        )

        return FSerialBleApiImpl(
            scope = scope,
            unsafeSerialApi = unsafeApi,
        )
    }
}
