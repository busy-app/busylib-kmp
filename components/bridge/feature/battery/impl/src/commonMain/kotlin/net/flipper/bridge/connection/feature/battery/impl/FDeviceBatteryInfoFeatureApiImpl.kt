package net.flipper.bridge.connection.feature.battery.impl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import net.flipper.bridge.connection.feature.battery.api.FDeviceBatteryInfoFeatureApi
import net.flipper.bridge.connection.feature.battery.impl.delegates.FEventsBatteryInfoDelegate
import net.flipper.bridge.connection.feature.battery.impl.delegates.FGATTBatteryInfoDelegate
import net.flipper.bridge.connection.feature.battery.model.BSBDeviceBatteryInfo
import net.flipper.bridge.connection.feature.events.api.FEventsFeatureApi
import net.flipper.bridge.connection.feature.events.api.get
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import net.flipper.bridge.connection.transport.common.api.meta.FTransportMetaInfoApi
import net.flipper.busylib.core.wrapper.WrappedFlow
import net.flipper.busylib.core.wrapper.wrap
import net.flipper.core.busylib.log.info

class FDeviceBatteryInfoFeatureApiImpl(
    metaInfoApi: FTransportMetaInfoApi?,
    rpcFeatureApi: FRpcFeatureApi,
    eventsApi: FEventsFeatureApi,
    scope: CoroutineScope
) : FDeviceBatteryInfoFeatureApi {
    private val gattBatteryInfoDelegate = FGATTBatteryInfoDelegate(metaInfoApi)
    private val eventsBatteryInfoDelegate by lazy {
        FEventsBatteryInfoDelegate(
            eventsApi,
            scope,
            rpcFeatureApi
        )
    }

    override fun getDeviceBatteryInfo(): WrappedFlow<BSBDeviceBatteryInfo> {
        return gattBatteryInfoDelegate.getGattBatteryInfoFlow()
            .distinctUntilChanged()
            .flatMapLatest { batteryInfo ->
                info { "#getDeviceBatteryInfo: $batteryInfo" }
                if (batteryInfo == null) {
                    eventsBatteryInfoDelegate.rpcBatteryInfoFlow.filterNotNull()
                } else {
                    flowOf(batteryInfo)
                }
            }.wrap()
    }
}
