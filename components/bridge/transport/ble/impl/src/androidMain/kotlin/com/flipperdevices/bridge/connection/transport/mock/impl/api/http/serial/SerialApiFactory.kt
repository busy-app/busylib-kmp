package com.flipperdevices.bridge.connection.transport.mock.impl.api.http.serial

import com.flipperdevices.bridge.connection.transport.ble.api.FBleDeviceSerialConfig
import com.flipperdevices.core.busylib.log.LogTagProvider
import me.tatarka.inject.annotations.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import no.nordicsemi.kotlin.ble.client.RemoteService

@Inject
class SerialApiFactory(
    private val unsafeApiImplFactory: FSerialUnsafeApiImpl.Factory,
) : LogTagProvider {
    override val TAG = "SerialApiCombinedFactory"

    fun build(
        config: FBleDeviceSerialConfig,
        services: StateFlow<List<RemoteService>?>,
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

        return FSerialBleApi(
            scope = scope,
            unsafeSerialApi = unsafeApi,
        )
    }
}
