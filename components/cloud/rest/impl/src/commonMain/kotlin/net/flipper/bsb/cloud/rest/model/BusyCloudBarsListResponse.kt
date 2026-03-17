package net.flipper.bsb.cloud.rest.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class BusyCloudBarsListResponse(
    @SerialName("success")
    val success: BusyCloudBarsListSuccess
)

@Serializable
internal data class BusyCloudBarsListSuccess(
    @SerialName("bars")
    val bars: List<BusyCloudBar>
)
