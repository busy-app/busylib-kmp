package net.flipper.bridge.connection.feature.smarthome.model

import kotlin.time.Instant

data class BsbMatterCommissionedFabrics(
    val fabricCount: Int,
    val latestCommissioningStatus: BsbCommissioningStatus
) {
    data class BsbCommissioningStatus(
        val timestamp: Instant? = null,
        val value: BsbCommissioningStatusType
    )

    enum class BsbCommissioningStatusType {
        NEVER_STARTED,
        STARTED,
        COMPLETED_SUCCESSFULLY,
        FAILED
    }
}
