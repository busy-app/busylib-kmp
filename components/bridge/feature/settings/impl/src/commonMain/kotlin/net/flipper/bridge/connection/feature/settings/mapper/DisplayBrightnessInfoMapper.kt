package net.flipper.bridge.connection.feature.settings.mapper

import net.flipper.bridge.connection.feature.rpc.api.model.DisplayBrightnessInfo
import net.flipper.bridge.connection.feature.settings.model.BsbBrightness
import net.flipper.bridge.connection.feature.settings.model.BsbBrightnessInfo
import net.flipper.core.busylib.data.Fraction

internal fun DisplayBrightnessInfo.toBsbBrightnessInfo(): BsbBrightnessInfo {
    return BsbBrightnessInfo(
        value = this.value.toIntOrNull()
            ?.let(Fraction::fromWholePercent)
            ?.let(BsbBrightness::Number)
            ?: BsbBrightness.Auto,
    )
}

internal fun BsbBrightness.toDisplayBrightnessInfo(): DisplayBrightnessInfo {
    return when (this) {
        BsbBrightness.Auto -> DisplayBrightnessInfo("auto")
        is BsbBrightness.Number -> {
            val value = this.value.toWholePercent().toInt().toString()
            DisplayBrightnessInfo(value)
        }
    }
}
