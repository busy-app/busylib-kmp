package com.flipperdevices.bridge.connection.feature.wifi.impl

import com.flipperdevices.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import com.flipperdevices.bridge.connection.feature.rpc.api.model.ConnectRequestConfig
import com.flipperdevices.bridge.connection.feature.rpc.api.model.Network
import com.flipperdevices.bridge.connection.feature.rpc.api.model.WifiIpConfig
import com.flipperdevices.bridge.connection.feature.rpc.api.model.WifiIpMethod
import com.flipperdevices.bridge.connection.feature.rpc.api.model.WifiSecurityMethod
import com.flipperdevices.bridge.connection.feature.wifi.api.FWiFiFeatureApi
import com.flipperdevices.bridge.connection.feature.wifi.api.model.WiFiNetwork
import com.flipperdevices.bridge.connection.feature.wifi.api.model.WiFiSecurity
import com.flipperdevices.core.busylib.log.LogTagProvider
import com.flipperdevices.core.busylib.log.error
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.time.Duration.Companion.seconds

private val POOLING_TIME = 3.seconds

class FWiFiFeatureApiImpl(
    private val rpcFeatureApi: FRpcFeatureApi
) : FWiFiFeatureApi, LogTagProvider {
    override val TAG = "FWiFiFeatureApi"

    override suspend fun getWifiStateFlow(): Flow<ImmutableList<WiFiNetwork>> {
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
        }
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

    interface InternalFactory {
        operator fun invoke(
            rpcFeatureApi: FRpcFeatureApi
        ): FWiFiFeatureApiImpl
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
