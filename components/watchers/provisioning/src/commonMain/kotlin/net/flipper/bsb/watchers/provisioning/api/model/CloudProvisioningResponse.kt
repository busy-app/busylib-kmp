package net.flipper.bsb.watchers.provisioning.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CloudProvisioningResponse(
    @SerialName("success")
    val success: CloudProvisioningSuccess
)

@Serializable
data class CloudProvisioningSuccess(
    @SerialName("bars")
    val bars: List<CloudProvisioningBar>
)

@Serializable
data class CloudProvisioningBar(
    @SerialName("id")
    val id: String,
    @SerialName("label")
    val label: String? = null
)
