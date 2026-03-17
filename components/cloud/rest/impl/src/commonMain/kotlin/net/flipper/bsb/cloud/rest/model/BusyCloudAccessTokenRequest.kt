package net.flipper.bsb.cloud.rest.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class BusyCloudAccessTokenRequest(
    @SerialName("scope")
    val scopes: List<String> = listOf("http:read", "http:write", "http:delete"),
    @SerialName("ttl_seconds")
    val ttlSeconds: Long = 600
)
