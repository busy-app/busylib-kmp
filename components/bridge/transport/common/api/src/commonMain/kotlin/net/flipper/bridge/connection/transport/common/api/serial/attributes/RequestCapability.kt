package net.flipper.bridge.connection.transport.common.api.serial.attributes

import io.ktor.client.request.HttpRequestBuilder
import io.ktor.util.AttributeKey
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import net.flipper.bridge.connection.transport.common.api.serial.FHTTPTransportCapability

val RequestCapabilityKey = AttributeKey<ImmutableSet<FHTTPTransportCapability>>("RequestCapability")

/**
 * Restricts this request to transports that expose every capability in [capabilities].
 *
 * The combined HTTP engine reads [RequestCapabilityKey] and only dispatches the request to
 * delegates whose capability set contains all of the requested capabilities.
 */
fun HttpRequestBuilder.requireCapabilities(vararg capabilities: FHTTPTransportCapability) {
    attributes.put(RequestCapabilityKey, persistentSetOf(*capabilities))
}

/**
 * Restricts this request to a local (LAN/BLE) connection.
 *
 * Shorthand for [requireCapabilities] with [FHTTPTransportCapability.BB_LOCAL_CONNECTION].
 */
fun HttpRequestBuilder.requireLocalConnection() {
    requireCapabilities(FHTTPTransportCapability.BB_LOCAL_CONNECTION)
}
