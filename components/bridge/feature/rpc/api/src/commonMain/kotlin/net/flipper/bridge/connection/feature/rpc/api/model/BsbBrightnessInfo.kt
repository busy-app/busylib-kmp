package net.flipper.bridge.connection.feature.rpc.api.model

data class BsbBrightnessInfo(
    val front: BsbBrightness,
)

fun DisplayBrightnessInfo.toBsbBrightnessInfo(): BsbBrightnessInfo {
    return BsbBrightnessInfo(
        front = this.front.toIntOrNull()
            ?.let(BsbBrightness::Number)
            ?: BsbBrightness.Auto,
    )
}
