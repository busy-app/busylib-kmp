package net.flipper.bridge.connection.transport.mock.impl.meta

import io.ktor.utils.io.core.toByteArray
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import net.flipper.bridge.connection.transport.common.api.meta.FTransportMetaInfoApi
import net.flipper.bridge.connection.transport.common.api.meta.TransportMetaInfoData
import net.flipper.bridge.connection.transport.common.api.meta.TransportMetaInfoKey

class MockFTransportMetaInfoApiImpl : FTransportMetaInfoApi {
    override fun get(key: TransportMetaInfoKey): Flow<Result<Flow<TransportMetaInfoData?>>> {
        val mockData = getData(key) ?: return flowOf(Result.failure(NotImplementedError()))
        return flowOf(Result.success(flowOf(mockData)))
    }

    private fun getData(key: TransportMetaInfoKey): TransportMetaInfoData? {
        return when (key) {
            TransportMetaInfoKey.DEVICE_NAME -> TransportMetaInfoData.RawBytes("Busy Bar".toByteArray())
            TransportMetaInfoKey.MANUFACTURER -> TransportMetaInfoData.RawBytes("Flipper Devices Inc.".toByteArray())
            TransportMetaInfoKey.HARDWARE_VERSION -> TransportMetaInfoData.RawBytes("11".toByteArray())
            TransportMetaInfoKey.SOFTWARE_VERSION -> TransportMetaInfoData.RawBytes("1.0.0 fakeusb0".toByteArray())
            TransportMetaInfoKey.BATTERY_LEVEL -> TransportMetaInfoData.RawBytes(byteArrayOf(0.5.toInt().toByte()))
            TransportMetaInfoKey.BATTERY_POWER_STATE -> TransportMetaInfoData.RawBytes(byteArrayOf(0))
            TransportMetaInfoKey.EVENTS_INDICATION -> TransportMetaInfoData.RawBytes(byteArrayOf(0))
            TransportMetaInfoKey.WS_EVENT -> null
        }
    }
}
