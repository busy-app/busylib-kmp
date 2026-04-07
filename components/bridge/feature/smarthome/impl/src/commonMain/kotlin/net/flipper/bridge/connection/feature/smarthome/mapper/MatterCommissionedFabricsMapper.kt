package net.flipper.bridge.connection.feature.smarthome.mapper

import net.flipper.bridge.connection.feature.rpc.api.model.MatterCommissionedFabrics
import net.flipper.bridge.connection.feature.rpc.api.model.MatterCommissionedFabrics.CommissioningStatus
import net.flipper.bridge.connection.feature.rpc.api.model.MatterCommissionedFabrics.CommissioningStatusType
import net.flipper.bridge.connection.feature.smarthome.model.BsbMatterCommissionedFabrics
import net.flipper.bridge.connection.feature.smarthome.model.BsbMatterCommissionedFabrics.BsbCommissioningStatus
import net.flipper.bridge.connection.feature.smarthome.model.BsbMatterCommissionedFabrics.BsbCommissioningStatusType

private fun CommissioningStatus.toBsbCommissioningStatus(): BsbCommissioningStatus {
    return BsbCommissioningStatus(
        timestamp = timestamp,
        value = value.toBsbCommissioningStatusType()
    )
}

private fun BsbCommissioningStatus.toCommissioningStatus(): CommissioningStatus {
    return CommissioningStatus(
        timestamp = timestamp,
        value = value.toCommissioningStatusType()
    )
}

private fun CommissioningStatusType.toBsbCommissioningStatusType(): BsbCommissioningStatusType {
    return when (this) {
        CommissioningStatusType.NEVER_STARTED -> BsbCommissioningStatusType.NEVER_STARTED
        CommissioningStatusType.STARTED -> BsbCommissioningStatusType.STARTED
        CommissioningStatusType.COMPLETED_SUCCESSFULLY -> BsbCommissioningStatusType.COMPLETED_SUCCESSFULLY
        CommissioningStatusType.FAILED -> BsbCommissioningStatusType.FAILED
    }
}

private fun BsbCommissioningStatusType.toCommissioningStatusType(): CommissioningStatusType {
    return when (this) {
        BsbCommissioningStatusType.NEVER_STARTED -> CommissioningStatusType.NEVER_STARTED
        BsbCommissioningStatusType.STARTED -> CommissioningStatusType.STARTED
        BsbCommissioningStatusType.COMPLETED_SUCCESSFULLY -> CommissioningStatusType.COMPLETED_SUCCESSFULLY
        BsbCommissioningStatusType.FAILED -> CommissioningStatusType.FAILED
    }
}

internal fun MatterCommissionedFabrics.toBsbMatterCommissionedFabrics(): BsbMatterCommissionedFabrics {
    return BsbMatterCommissionedFabrics(
        fabricCount = fabricCount,
        latestCommissioningStatus = latestCommissioningStatus.toBsbCommissioningStatus()
    )
}

internal fun BsbMatterCommissionedFabrics.toMatterCommissionedFabrics(): MatterCommissionedFabrics {
    return MatterCommissionedFabrics(
        fabricCount = fabricCount,
        latestCommissioningStatus = latestCommissioningStatus.toCommissioningStatus()
    )
}
