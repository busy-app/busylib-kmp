package net.flipper.bridge.connection.feature.rpc.api.model

import kotlinx.serialization.Serializable
import net.flipper.bridge.connection.feature.rpc.api.serialization.BsbBrightnessSerializer

@Serializable
class DisplayBrightnessInfo(
    @Serializable(BsbBrightnessSerializer::class)
    val front: BsbBrightness,
    @Serializable(BsbBrightnessSerializer::class)
    val back: BsbBrightness,
)
