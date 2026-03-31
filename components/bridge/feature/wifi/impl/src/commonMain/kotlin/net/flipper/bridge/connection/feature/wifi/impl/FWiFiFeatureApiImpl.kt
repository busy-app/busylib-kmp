package net.flipper.bridge.connection.feature.wifi.impl

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import me.tatarka.inject.annotations.Inject
import me.tatarka.inject.annotations.IntoMap
import me.tatarka.inject.annotations.Provides
import net.flipper.bridge.connection.feature.common.api.FDeviceFeature
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.common.api.FUnsafeDeviceFeatureApi
import net.flipper.bridge.connection.feature.events.api.FEventsFeatureApi
import net.flipper.bridge.connection.feature.events.api.get
import net.flipper.bridge.connection.feature.events.model.BusyLibUpdateEvent
import net.flipper.bridge.connection.feature.events.model.ConsumableUpdateEvent
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.model.ConnectRequestConfig
import net.flipper.bridge.connection.feature.rpc.api.model.StatusResponse
import net.flipper.bridge.connection.feature.rpc.api.model.WifiIpMethod
import net.flipper.bridge.connection.feature.wifi.api.FWiFiFeatureApi
import net.flipper.bridge.connection.feature.wifi.api.model.WiFiNetwork
import net.flipper.bridge.connection.feature.wifi.api.model.WiFiSecurity
import net.flipper.bridge.connection.feature.wifi.util.toInternalSecurity
import net.flipper.bridge.connection.feature.wifi.util.toWiFiNetwork
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.bridge.connection.transport.common.api.serial.FHTTPDeviceApi
import net.flipper.bridge.connection.transport.common.api.serial.FHTTPTransportCapability
import net.flipper.bridge.connection.transport.common.api.serial.hasCapability
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.busylib.core.wrapper.CResult
import net.flipper.busylib.core.wrapper.WrappedFlow
import net.flipper.busylib.core.wrapper.toCResult
import net.flipper.busylib.core.wrapper.wrap
import net.flipper.core.busylib.ktx.common.asFlow
import net.flipper.core.busylib.ktx.common.exponentialRetry
import net.flipper.core.busylib.ktx.common.merge
import net.flipper.core.busylib.ktx.common.orElse
import net.flipper.core.busylib.ktx.common.orEmpty
import net.flipper.core.busylib.ktx.common.throttleLatestCached
import net.flipper.core.busylib.ktx.common.transformWhileSubscribed
import net.flipper.core.busylib.ktx.common.tryConsume
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.error
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo
import kotlin.time.Duration.Companion.seconds

private val POOLING_TIME = 3.seconds

class FWiFiFeatureApiImpl(
    private val rpcFeatureApi: FRpcFeatureApi,
    private val fEventsFeatureApi: FEventsFeatureApi?,
    private val scope: CoroutineScope,
    private val fHTTPDeviceApi: FHTTPDeviceApi?
) : FWiFiFeatureApi, LogTagProvider {
    override val TAG = "FWiFiFeatureApi"

    override fun getWifiStateFlow(): WrappedFlow<ImmutableList<WiFiNetwork>> {
        return callbackFlow {
            var networks = listOf<WiFiNetwork>()

            while (isActive) {
                val networksResponse = exponentialRetry {
                    rpcFeatureApi
                        .fRpcWifiApi
                        .getWifiNetworks()
                        .onFailure { error(it) { "Failed to get WiFi networks" } }
                }
                val mutableNetworkList = networksResponse.networks
                    .map { it.toWiFiNetwork() }
                    .groupBy { it.ssid }
                    .map { (_, networks) -> networks.maxWith(WiFiNetworkReplaceComparator()) }
                    .toMutableList()
                networks = networks.map { storedNetwork ->
                    val updatedNetwork = mutableNetworkList.find { storedNetwork.ssid == it.ssid }
                    if (updatedNetwork != null) {
                        mutableNetworkList.remove(updatedNetwork)
                        updatedNetwork
                    } else {
                        storedNetwork
                    }
                } + mutableNetworkList

                send(networks.toPersistentList())

                delay(POOLING_TIME)
            }
        }.wrap()
    }

    private val wifiStatusSharedFlow = fEventsFeatureApi
        ?.get<BusyLibUpdateEvent.Wifi>()
        .orEmpty()
        .merge(flowOf(ConsumableUpdateEvent.Empty))
        .transformWhileSubscribed(scope = scope) { flow ->
            flow.throttleLatestCached { consumable, wifi: StatusResponse? ->
                val couldConsume = consumable.tryConsume()
                when (consumable) {
                    is ConsumableUpdateEvent.BusyLib<BusyLibUpdateEvent.Wifi> if wifi != null -> {
                        consumable.busyLibUpdateEvent.let { wifiUpdateEvent ->
                            wifi.copy(
                                // todo fix wifi proto model mapping?
                                state = when {
                                    wifiUpdateEvent.isConnected -> StatusResponse.State.CONNECTED
                                    else -> wifi.state
                                },
                                ssid = wifi.ssid,
                            )
                        }
                    }

                    else -> {
                        exponentialRetry {
                            rpcFeatureApi
                                .fRpcWifiApi
                                .getWifiStatus(couldConsume)
                                .onFailure { error(it) { "Failed to get WiFi networks" } }
                        }
                    }
                }
            }
        }
        .asFlow()
        .wrap()

    override fun getWifiStatusFlow(): WrappedFlow<StatusResponse> {
        return wifiStatusSharedFlow
    }

    override suspend fun connect(
        ssid: String,
        password: String,
        security: WiFiSecurity.Supported
    ): CResult<Unit> {
        return rpcFeatureApi.fRpcWifiApi.connectWifi(
            ConnectRequestConfig(
                ssid = ssid,
                password = password,
                security = security.toInternalSecurity(),
                ipConfig = ConnectRequestConfig.IpConfig(
                    ipMethod = WifiIpMethod.DHCP
                )
            )
        ).map { }.toCResult()
    }

    override suspend fun disconnect(): CResult<Unit> {
        return rpcFeatureApi.fRpcWifiApi.disconnectWifi().map { }.toCResult()
    }

    override val isWifiEditingAllowed = fHTTPDeviceApi
        ?.hasCapability(FHTTPTransportCapability.BB_LOCAL_CONNECTION)
        .orElse { false }
        .wrap()

    @Inject
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
            val fEventsFeatureApi = unsafeFeatureDeviceApi
                .get(FEventsFeatureApi::class)
                ?.await()

            return FWiFiFeatureApiImpl(
                rpcFeatureApi = fRpcFeatureApi,
                fEventsFeatureApi = fEventsFeatureApi,
                scope = scope,
                fHTTPDeviceApi = connectedDevice as? FHTTPDeviceApi
            )
        }
    }

    @ContributesTo(BusyLibGraph::class)
    interface Component {
        @Provides
        @IntoMap
        fun provideFeatureFactory(
            factory: Factory
        ): Pair<FDeviceFeature, FDeviceFeatureApi.Factory> {
            return FDeviceFeature.WIFI to factory
        }
    }
}
