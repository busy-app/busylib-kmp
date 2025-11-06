package com.flipperdevices.bsb.auth.principal.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class PreferenceBsbUser(
    @SerialName("email")
    val email: String,
    @SerialName("user_id")
    val userId: String? = null
)
