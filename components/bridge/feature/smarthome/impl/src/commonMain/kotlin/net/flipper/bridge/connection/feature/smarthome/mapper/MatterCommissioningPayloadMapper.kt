package net.flipper.bridge.connection.feature.smarthome.mapper

import net.flipper.bridge.connection.feature.rpc.api.model.MatterCommissioningPayload
import net.flipper.bridge.connection.feature.smarthome.model.BsbMatterCommissioningPayload

internal fun MatterCommissioningPayload.toBsbMatterCommissioningPayload(): BsbMatterCommissioningPayload {
    return BsbMatterCommissioningPayload(
        availableUntil = availableUntil,
        qrCode = qrCode,
        manualCode = manualCode
    )
}

internal fun BsbMatterCommissioningPayload.toMatterCommissioningPayload(): MatterCommissioningPayload {
    return MatterCommissioningPayload(
        availableUntil = availableUntil,
        qrCode = qrCode,
        manualCode = manualCode
    )
}
