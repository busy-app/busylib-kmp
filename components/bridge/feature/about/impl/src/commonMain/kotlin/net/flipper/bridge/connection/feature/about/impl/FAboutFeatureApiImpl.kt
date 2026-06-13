package net.flipper.bridge.connection.feature.about.impl

import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import kotlinx.coroutines.CoroutineScope
import net.flipper.bridge.connection.feature.about.api.FAboutFeatureApi
import net.flipper.bridge.connection.feature.about.model.BusyBarAboutDevice
import net.flipper.bridge.connection.feature.common.api.FDeviceFeature
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureKey
import net.flipper.bridge.connection.feature.common.api.FUnsafeDeviceFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.busylib.core.wrapper.CResult
import net.flipper.busylib.core.wrapper.toCResult

class FAboutFeatureApiImpl(
    private val rpcFeatureApi: FRpcFeatureApi
) : FAboutFeatureApi {
    override suspend fun getAboutDevice(): CResult<BusyBarAboutDevice> {
        return rpcFeatureApi.fRpcSystemApi
            .getStatus()
            .map { status ->
                BusyBarAboutDevice(
                    serialNumber = status.device.serialNumber,
                    macAddressBluetooth = status.device.bleMac,
                    macAddressWifi = status.device.wifiMac,
                    macAddressUsb = status.device.usbMac,
                    hardwareVersion = null,
                    productionDate = null,
                    frontDisplayResolution = FRONT_DISPLAY_RESOLUTION,
                    frontDisplayRefreshRate = FRONT_DISPLAY_REFRESH_RATE,
                    backDisplayResolution = BACK_DISPLAY_RESOLUTION,
                    centralMcu = CENTRAL_MCU,
                    ramSize = RAM_SIZE
                )
            }
            .toCResult()
    }

    companion object {
        private const val FRONT_DISPLAY_RESOLUTION = "72×16 (LED)"
        private const val FRONT_DISPLAY_REFRESH_RATE = "60 Hz"
        private const val BACK_DISPLAY_RESOLUTION = "160×80 (OLED)"
        private const val CENTRAL_MCU = "STM32U5M"
        private const val RAM_SIZE = "2.5 MB"
    }

    @Inject
    @ContributesIntoMap(BusyLibGraph::class, binding<FDeviceFeatureApi.Factory>())
    @FDeviceFeatureKey(FDeviceFeature.ABOUT)
    class Factory : FDeviceFeatureApi.Factory {
        override suspend fun invoke(
            unsafeFeatureDeviceApi: FUnsafeDeviceFeatureApi,
            scope: CoroutineScope,
            connectedDevice: FConnectedDeviceApi
        ): FDeviceFeatureApi? {
            val fRpcFeatureApi = unsafeFeatureDeviceApi
                .get(FRpcFeatureApi::class)
                ?.await()
                ?: return null

            return FAboutFeatureApiImpl(fRpcFeatureApi)
        }
    }
}
