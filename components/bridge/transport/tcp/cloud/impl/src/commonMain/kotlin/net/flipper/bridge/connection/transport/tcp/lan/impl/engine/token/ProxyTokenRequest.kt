package net.flipper.bridge.connection.transport.tcp.lan.impl.engine.token

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProxyTokenRequest(
    @SerialName("scope")
    val scopes: List<String> = listOf("http:read", "http:write", "http:delete"),
    @SerialName("ttl_seconds")
    val ttlSeconds: Long
)
