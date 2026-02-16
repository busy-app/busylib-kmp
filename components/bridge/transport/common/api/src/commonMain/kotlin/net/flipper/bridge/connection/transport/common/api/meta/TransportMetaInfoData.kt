package net.flipper.bridge.connection.transport.common.api.meta

sealed interface TransportMetaInfoData {
    data class RawBytes(
        val bytes: ByteArray
    ) : TransportMetaInfoData {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as RawBytes

            if (!bytes.contentEquals(other.bytes)) return false

            return true
        }

        override fun hashCode(): Int {
            return bytes.contentHashCode()
        }
    }
}