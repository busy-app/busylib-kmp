package net.flipper.bridge.connection.feature.rpc.api.model

data class BsbBrightnessInfo(
    val value: BsbBrightness,
)

fun DisplayBrightnessInfo.toBsbBrightnessInfo(): BsbBrightnessInfo {
    return BsbBrightnessInfo(
        value = this.value.toIntOrNull()
            ?.let(BsbBrightness::Number)
            ?: BsbBrightness.Auto,
    )
}
