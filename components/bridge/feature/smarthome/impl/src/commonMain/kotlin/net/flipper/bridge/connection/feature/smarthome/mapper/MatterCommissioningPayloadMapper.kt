package net.flipper.bridge.connection.feature.smarthome.mapper

import net.flipper.bridge.connection.feature.rpc.generated.model.SmartHomePairingPayload
import net.flipper.bridge.connection.feature.smarthome.model.BsbMatterCommissioningPayload

internal fun SmartHomePairingPayload.toBsbMatterCommissioningPayload(): BsbMatterCommissioningPayload {
    return BsbMatterCommissioningPayload(
        availableUntil = availableUntil,
        qrCode = qrCode,
        manualCode = manualCode
    )
}

internal fun BsbMatterCommissioningPayload.toMatterCommissioningPayload(): SmartHomePairingPayload {
    return SmartHomePairingPayload(
        availableUntil = availableUntil,
        qrCode = qrCode,
        manualCode = manualCode
    )
}
