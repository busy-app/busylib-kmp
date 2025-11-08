package com.flipperdevices.busylib.di

import com.flipperdevices.bridge.connection.feature.battery.impl.FDeviceBatteryInfoFeatureApiImpl
import com.flipperdevices.bridge.connection.feature.battery.impl.FDeviceBatteryInfoFeatureFactoryImpl
import com.flipperdevices.bridge.connection.feature.common.api.FDeviceFeature
import com.flipperdevices.bridge.connection.feature.common.api.FDeviceFeatureApi
import com.flipperdevices.bridge.connection.feature.firmwareupdate.impl.FFirmwareUpdateFeatureApiImpl
import com.flipperdevices.bridge.connection.feature.firmwareupdate.impl.FFirmwareUpdateFeatureFactoryImpl
import com.flipperdevices.bridge.connection.feature.info.impl.FDeviceInfoFeatureApiImpl
import com.flipperdevices.bridge.connection.feature.info.impl.FDeviceInfoFeatureFactoryImpl
import com.flipperdevices.bridge.connection.feature.link.check.ondemand.api.FLinkInfoOnDemandFeatureApiImpl
import com.flipperdevices.bridge.connection.feature.link.check.ondemand.api.FLinkInfoOnDemandFeatureFactoryImpl
import com.flipperdevices.bridge.connection.feature.rpc.api.critical.FRpcCriticalFeatureApi
import com.flipperdevices.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import com.flipperdevices.bridge.connection.feature.rpc.impl.critical.FRpcCriticalFeatureApiImpl
import com.flipperdevices.bridge.connection.feature.rpc.impl.critical.FRpcCriticalFeatureFactoryImpl
import com.flipperdevices.bridge.connection.feature.rpc.impl.exposed.FRpcFeatureApiFactoryImpl
import com.flipperdevices.bridge.connection.feature.rpc.impl.exposed.FRpcFeatureApiImpl
import com.flipperdevices.bridge.connection.feature.screenstreaming.impl.FScreenStreamingFeatureApiImpl
import com.flipperdevices.bridge.connection.feature.screenstreaming.impl.FScreenStreamingFeatureFactoryImpl
import com.flipperdevices.bridge.connection.feature.wifi.impl.FWiFiFeatureApiImpl
import com.flipperdevices.bridge.connection.feature.wifi.impl.FWiFiFeatureFactoryImpl
import com.flipperdevices.bridge.connection.transport.common.api.meta.FTransportMetaInfoApi
import com.flipperdevices.bsb.auth.principal.api.BsbUserPrincipalApi
import com.flipperdevices.bsb.cloud.api.BSBBarsApi
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope

class FDeviceFeatureModule(
    private val bsbUserPrincipalApi: BsbUserPrincipalApi,
    private val bsbBarsApi: BSBBarsApi
) {

    val fRpcCriticalFeatureFactoryImpl: FDeviceFeatureApi.Factory = FRpcCriticalFeatureFactoryImpl(
        fRpcCriticalFeatureApiFactory = object : FRpcCriticalFeatureApiImpl.InternalFactory {
            override fun invoke(client: HttpClient): FRpcCriticalFeatureApiImpl {
                return FRpcCriticalFeatureApiImpl(client)
            }
        }
    )
    val fRpcFeatureApiFactoryImpl = FRpcFeatureApiFactoryImpl(
        fRpcFeatureFactory = object : FRpcFeatureApiImpl.InternalFactory {
            override fun invoke(client: HttpClient): FRpcFeatureApiImpl {
                return FRpcFeatureApiImpl(client)
            }
        }
    )
    val fDeviceInfoFeatureFactoryImpl = FDeviceInfoFeatureFactoryImpl(
        deviceInfoFeatureFactory = object : FDeviceInfoFeatureApiImpl.InternalFactory {
            override fun invoke(rpcFeatureApi: FRpcFeatureApi): FDeviceInfoFeatureApiImpl {
                return FDeviceInfoFeatureApiImpl(rpcFeatureApi)
            }
        }
    )
    val fDeviceBatteryInfoFeatureFactoryImpl = FDeviceBatteryInfoFeatureFactoryImpl(
        deviceInfoFeatureFactory = object : FDeviceBatteryInfoFeatureApiImpl.InternalFactory {
            override fun invoke(
                rpcFeatureApi: FRpcFeatureApi,
                metaInfoApi: FTransportMetaInfoApi?
            ): FDeviceBatteryInfoFeatureApiImpl {
                return FDeviceBatteryInfoFeatureApiImpl(
                    rpcFeatureApi = rpcFeatureApi,
                    metaInfoApi = metaInfoApi
                )
            }
        }
    )
    val fWiFiFeatureFactoryImpl = FWiFiFeatureFactoryImpl(
        deviceInfoFeatureFactory = object : FWiFiFeatureApiImpl.InternalFactory {
            override fun invoke(rpcFeatureApi: FRpcFeatureApi): FWiFiFeatureApiImpl {
                return FWiFiFeatureApiImpl(rpcFeatureApi)
            }
        }
    )
    val fScreenStreamingFeatureFactoryImpl = FScreenStreamingFeatureFactoryImpl(
        internalFactory = object : FScreenStreamingFeatureApiImpl.InternalFactory {
            override fun invoke(rpcFeatureApi: FRpcFeatureApi): FScreenStreamingFeatureApiImpl {
                return FScreenStreamingFeatureApiImpl(rpcFeatureApi)
            }
        }
    )

    val fFirmwareUpdateFeatureFactoryImpl = FFirmwareUpdateFeatureFactoryImpl(
        internalFactory = object : FFirmwareUpdateFeatureApiImpl.InternalFactory {
            override fun invoke(rpcFeatureApi: FRpcFeatureApi): FFirmwareUpdateFeatureApiImpl {
                return FFirmwareUpdateFeatureApiImpl(rpcFeatureApi)
            }
        }
    )
    val fLinkInfoOnDemandFeatureFactoryImpl = FLinkInfoOnDemandFeatureFactoryImpl(
        linkedInfoFeatureFactory = object : FLinkInfoOnDemandFeatureApiImpl.InternalFactory {
            override fun invoke(
                rpcFeatureApi: FRpcCriticalFeatureApi,
                scope: CoroutineScope
            ): FLinkInfoOnDemandFeatureApiImpl {
                return FLinkInfoOnDemandFeatureApiImpl(
                    rpcFeatureApi = rpcFeatureApi,
                    scope = scope,
                    bsbUserPrincipalApi = bsbUserPrincipalApi,
                    bsbBarsApi = bsbBarsApi
                )
            }
        }
    )

    val factories = FDeviceFeature.entries.associateWith { feature ->
        when (feature) {
            FDeviceFeature.RPC_EXPOSED -> fRpcFeatureApiFactoryImpl
            FDeviceFeature.RPC_CRITICAL -> fRpcCriticalFeatureFactoryImpl
            FDeviceFeature.DEVICE_INFO -> fDeviceInfoFeatureFactoryImpl
            FDeviceFeature.BATTERY_INFO -> fDeviceBatteryInfoFeatureFactoryImpl
            FDeviceFeature.WIFI -> fWiFiFeatureFactoryImpl
            FDeviceFeature.SCREEN_STREAMING -> fScreenStreamingFeatureFactoryImpl
            FDeviceFeature.FIRMWARE_UPDATE -> fFirmwareUpdateFeatureFactoryImpl
            FDeviceFeature.LINKED_USER_STATUS -> fLinkInfoOnDemandFeatureFactoryImpl
        }
    }
}
