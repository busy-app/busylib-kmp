package net.flipper.bridge.connection.transport.common.api.meta

sealed interface TransportMetaInfoData {
    data class RawBytes(
        val bytes: ByteArray
    ) : TransportMetaInfoData {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as RawBytes

            return bytes.contentEquals(other.bytes)
        }

        override fun hashCode(): Int {
            return bytes.contentHashCode()
        }
    }

    data class StringValue(
        val key: String,
        val value: String
    ) : TransportMetaInfoData
}
