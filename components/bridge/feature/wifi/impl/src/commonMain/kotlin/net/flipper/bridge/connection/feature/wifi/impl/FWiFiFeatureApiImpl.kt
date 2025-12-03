package net.flipper.bridge.connection.feature.wifi.impl

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.model.ConnectRequestConfig
import net.flipper.bridge.connection.feature.rpc.api.model.Network
import net.flipper.bridge.connection.feature.rpc.api.model.WifiIpConfig
import net.flipper.bridge.connection.feature.rpc.api.model.WifiIpMethod
import net.flipper.bridge.connection.feature.rpc.api.model.WifiSecurityMethod
import net.flipper.bridge.connection.feature.rpc.api.model.WifiStatusResponse
import net.flipper.bridge.connection.feature.wifi.api.FWiFiFeatureApi
import net.flipper.bridge.connection.feature.wifi.api.model.WiFiNetwork
import net.flipper.bridge.connection.feature.wifi.api.model.WiFiSecurity
import net.flipper.busylib.core.wrapper.WrappedFlow
import net.flipper.busylib.core.wrapper.wrap
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.error
import kotlin.time.Duration.Companion.seconds

private val POOLING_TIME = 3.seconds

@Inject
class FWiFiFeatureApiImpl(
    @Assisted private val rpcFeatureApi: FRpcFeatureApi
) : FWiFiFeatureApi, LogTagProvider {
    override val TAG = "FWiFiFeatureApi"

    override fun getWifiStateFlow(): WrappedFlow<ImmutableList<WiFiNetwork>> {
        return callbackFlow {
            var networks = listOf<WiFiNetwork>()

            while (isActive) {
                val networksResponse = rpcFeatureApi.getWifiNetworks().onFailure {
                    error(it) { "Failed to get WiFi networks" }
                }.getOrNull() ?: continue
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

    override fun getWifiStatusFlow(): WrappedFlow<WifiStatusResponse> {
        return callbackFlow {
            while (isActive) {
                val statusResponse = rpcFeatureApi.getWifiStatus().onFailure {
                    error(it) { "Failed to get WiFi networks" }
                }.getOrNull() ?: continue

                send(statusResponse)

                delay(POOLING_TIME)
            }
        }.wrap()
    }

    override suspend fun connect(
        ssid: String,
        password: String,
        security: WiFiSecurity.Supported
    ): Result<Unit> {
        return rpcFeatureApi.connectWifi(
            ConnectRequestConfig(
                ssid = ssid,
                password = password,
                security = security.toInternalSecurity(),
                ipConfig = WifiIpConfig(
                    ipMethod = WifiIpMethod.DHCP
                )
            )
        ).map { }
    }

    override suspend fun disconnect(): Result<Unit> {
        return rpcFeatureApi.disconnectWifi().map { }
    }

    @Inject
    class InternalFactory(
        private val factory: (FRpcFeatureApi) -> FWiFiFeatureApiImpl
    ) {
        operator fun invoke(
            rpcFeatureApi: FRpcFeatureApi
        ): FWiFiFeatureApiImpl = factory(rpcFeatureApi)
    }
}

private fun Network.toWiFiNetwork(): WiFiNetwork {
    return WiFiNetwork(
        ssid = ssid,
        rssi = rssi,
        wifiSecurity = when (security) {
            WifiSecurityMethod.OPEN -> WiFiSecurity.Supported.None
            else -> WiFiSecurity.Supported.Password.fromInternal(
                security
            ) ?: WiFiSecurity.Other(security)
        }
    )
}

private fun WiFiSecurity.Supported.toInternalSecurity(): WifiSecurityMethod {
    return when (this) {
        WiFiSecurity.Supported.None -> WifiSecurityMethod.OPEN
        is WiFiSecurity.Supported.Password -> internalWifiSecurity
    }
}
