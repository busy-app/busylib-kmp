package net.flipper.bridge.connection.transport.common.api.serial.attributes

import io.ktor.util.AttributeKey
import net.flipper.bridge.connection.transport.common.api.serial.FHTTPTransportCapability

val RequestCapabilityKey = AttributeKey<List<FHTTPTransportCapability>>("RequestCapability")
