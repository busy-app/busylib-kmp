package net.flipper.bridge.connection.feature.smarthome.model

data class BsbMatterCommissionedFabrics(
    val fabricCount: Int,
    val latestCommissioningStatus: BsbCommissioningStatus
) {
    data class BsbCommissioningStatus(
        val value: BsbCommissioningStatusType
    )

    enum class BsbCommissioningStatusType {
        NEVER_STARTED,
        STARTED,
        COMPLETED_SUCCESSFULLY,
        FAILED
    }
}
