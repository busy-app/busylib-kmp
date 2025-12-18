package net.flipper.bridge.connection.feature.rpc.api.model

/**
 * Don't put @Serializable here
 *
 * @see net.flipper.bridge.connection.feature.rpc.api.serialization.BsbBrightnessSerializer
 * @see DisplayBrightnessInfo
 */
sealed interface BsbBrightness {
    data object Auto : BsbBrightness

    /**
     * @param value percentage from 0 to 100
     */
    data class Number(val value: Int) : BsbBrightness
}
