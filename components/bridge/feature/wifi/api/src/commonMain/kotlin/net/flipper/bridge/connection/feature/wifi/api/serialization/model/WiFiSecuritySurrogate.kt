package net.flipper.bridge.connection.feature.wifi.api.serialization.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import net.flipper.bridge.connection.feature.wifi.api.model.BsbWifiSecurityMethod
import net.flipper.bridge.connection.feature.wifi.api.model.WiFiSecurity

/**
 * @param isSupported `false` only for [WiFiSecurity.Other]; it keeps `Other(WPA2)` distinct from the
 * [WiFiSecurity.Supported.Password] entry carrying the very same method.
 */
@Serializable
internal data class WiFiSecuritySurrogate(
    @SerialName("is_supported")
    val isSupported: Boolean,
    @SerialName("internal_wifi_security")
    val internalWifiSecurity: BsbWifiSecurityMethod
)

internal fun WiFiSecurity.toSurrogate(): WiFiSecuritySurrogate {
    return when (this) {
        WiFiSecurity.Supported.None -> WiFiSecuritySurrogate(
            isSupported = true,
            internalWifiSecurity = BsbWifiSecurityMethod.OPEN
        )

        is WiFiSecurity.Supported.Password -> WiFiSecuritySurrogate(
            isSupported = true,
            internalWifiSecurity = internalWifiSecurity
        )

        is WiFiSecurity.Other -> WiFiSecuritySurrogate(
            isSupported = false,
            internalWifiSecurity = internalWifiSecurity
        )
    }
}

/**
 * @throws SerializationException if the payload does not describe a [WiFiSecurity.Supported] value
 */
internal fun WiFiSecuritySurrogate.toSupportedWiFiSecurity(): WiFiSecurity.Supported {
    if (!isSupported) {
        throw SerializationException("$internalWifiSecurity is not a supported wifi security method")
    }
    if (internalWifiSecurity == BsbWifiSecurityMethod.OPEN) {
        return WiFiSecurity.Supported.None
    }
    return WiFiSecurity.Supported.Password
        .entries
        .firstOrNull { password -> password.internalWifiSecurity == internalWifiSecurity }
        ?: throw SerializationException("No password protected wifi security matches $internalWifiSecurity")
}

internal fun WiFiSecuritySurrogate.toWiFiSecurity(): WiFiSecurity {
    if (!isSupported) {
        return WiFiSecurity.Other(internalWifiSecurity)
    }
    return toSupportedWiFiSecurity()
}
