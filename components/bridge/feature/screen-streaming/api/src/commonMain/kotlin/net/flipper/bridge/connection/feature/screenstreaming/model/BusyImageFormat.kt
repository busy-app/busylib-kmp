package net.flipper.bridge.connection.feature.screenstreaming.model

/**
 * This is not default base64
 * We need to convert is manually
 * Ask firmware developers for more
 */
data class BusyImageFormat(val byteArray: ByteArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BusyImageFormat

        return byteArray.contentEquals(other.byteArray)
    }

    override fun hashCode(): Int {
        return byteArray.contentHashCode()
    }
}
