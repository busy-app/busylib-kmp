package net.flipper.bridge.connection.feature.smarthome.mapper

import net.flipper.bridge.connection.feature.rpc.generated.model.SmartHomePairingInfo
import net.flipper.bridge.connection.feature.rpc.generated.model.SmartHomePairingInfoLatestPairingStatus
import net.flipper.bridge.connection.feature.smarthome.model.BsbMatterCommissionedFabrics
import net.flipper.bridge.connection.feature.smarthome.model.BsbMatterCommissionedFabrics.BsbCommissioningStatus
import net.flipper.bridge.connection.feature.smarthome.model.BsbMatterCommissionedFabrics.BsbCommissioningStatusType

private fun SmartHomePairingInfoLatestPairingStatus.toBsbCommissioningStatus(): BsbCommissioningStatus {
    return BsbCommissioningStatus(
        value = value.toBsbCommissioningStatusType()
    )
}

private fun BsbCommissioningStatus.toCommissioningStatus(): SmartHomePairingInfoLatestPairingStatus {
    return SmartHomePairingInfoLatestPairingStatus(
        value = value.toCommissioningStatusType()
    )
}

private fun SmartHomePairingInfoLatestPairingStatus.Value.toBsbCommissioningStatusType(): BsbCommissioningStatusType {
    return when (this) {
        SmartHomePairingInfoLatestPairingStatus.Value.NEVER_STARTED -> BsbCommissioningStatusType.NEVER_STARTED
        SmartHomePairingInfoLatestPairingStatus.Value.STARTED -> BsbCommissioningStatusType.STARTED
        SmartHomePairingInfoLatestPairingStatus.Value.COMPLETED_SUCCESSFULLY -> BsbCommissioningStatusType.COMPLETED_SUCCESSFULLY
        SmartHomePairingInfoLatestPairingStatus.Value.FAILED -> BsbCommissioningStatusType.FAILED
    }
}

private fun BsbCommissioningStatusType.toCommissioningStatusType(): SmartHomePairingInfoLatestPairingStatus.Value {
    return when (this) {
        BsbCommissioningStatusType.NEVER_STARTED -> SmartHomePairingInfoLatestPairingStatus.Value.NEVER_STARTED
        BsbCommissioningStatusType.STARTED -> SmartHomePairingInfoLatestPairingStatus.Value.STARTED
        BsbCommissioningStatusType.COMPLETED_SUCCESSFULLY -> SmartHomePairingInfoLatestPairingStatus.Value.COMPLETED_SUCCESSFULLY
        BsbCommissioningStatusType.FAILED -> SmartHomePairingInfoLatestPairingStatus.Value.FAILED
    }
}

internal fun SmartHomePairingInfo.toBsbMatterCommissionedFabrics(): BsbMatterCommissionedFabrics {
    return BsbMatterCommissionedFabrics(
        fabricCount = fabricCount,
        latestCommissioningStatus = latestPairingStatus.toBsbCommissioningStatus()
    )
}

internal fun BsbMatterCommissionedFabrics.toMatterCommissionedFabrics(): SmartHomePairingInfo {
    return SmartHomePairingInfo(
        fabricCount = fabricCount,
        latestPairingStatus = latestCommissioningStatus.toCommissioningStatus()
    )
}
