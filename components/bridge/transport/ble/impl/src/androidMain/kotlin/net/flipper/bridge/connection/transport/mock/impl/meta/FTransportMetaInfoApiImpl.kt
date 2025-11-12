package net.flipper.bridge.connection.transport.mock.impl.meta

import android.annotation.SuppressLint
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.merge
import net.flipper.bridge.connection.transport.ble.api.GATTCharacteristicAddress
import net.flipper.bridge.connection.transport.common.api.meta.FTransportMetaInfoApi
import net.flipper.bridge.connection.transport.common.api.meta.TransportMetaInfoKey
import net.flipper.core.busylib.ktx.common.WrappedStateFlow
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.info
import net.flipper.core.busylib.log.warn
import no.nordicsemi.kotlin.ble.client.RemoteService
import no.nordicsemi.kotlin.ble.core.CharacteristicProperty

class FTransportMetaInfoApiImpl(
    private val services: WrappedStateFlow<List<RemoteService>?>,
    private val metaInfoGattMap: ImmutableMap<TransportMetaInfoKey, GATTCharacteristicAddress>
) : FTransportMetaInfoApi, LogTagProvider {
    override val TAG = "FTransportMetaInfoApi"

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun get(key: TransportMetaInfoKey): Result<Flow<ByteArray?>> = runCatching {
        val address = metaInfoGattMap[key]
            ?: return Result.failure(RuntimeException("Can't found provider for $key"))

        val flow = services.flatMapLatest {
            getFlow(it, address)
        }
        return@runCatching flow
    }

    @SuppressLint("MissingPermission")
    private suspend fun getFlow(
        bleGattServices: List<RemoteService>?,
        address: GATTCharacteristicAddress
    ): Flow<ByteArray?> {
        val bleGattService = bleGattServices?.find { it.uuid == address.serviceAddress }
        info { "Found ble gatt service: ${bleGattService?.uuid}" }
        val characteristic = bleGattService?.characteristics?.find {
            it.uuid == address.characteristicAddress
        }

        if (characteristic == null) {
            warn { "Failed found gatt characteristic for $address" }
            return flowOf(null)
        }
        characteristic

        if (characteristic.properties.contains(CharacteristicProperty.NOTIFY).not() &&
            characteristic.properties.contains(CharacteristicProperty.INDICATE).not() &&
            characteristic.isNotifying.not()
        ) {
            warn {
                "Not found PROPERTY_NOTIFY or PROPERTY_INDICATE for property $address, " +
                    "so fallback on one-time read value"
            }
            return flowOf(characteristic.read())
        }
        info { "Subscribe on $address characteristic" }
        // Don't block one flow by another
        return listOf(
            flow { emit(characteristic.read()) },
            characteristic.subscribe()
        ).merge()
    }
}
