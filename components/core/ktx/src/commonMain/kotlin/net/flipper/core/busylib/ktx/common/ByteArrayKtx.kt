package net.flipper.core.busylib.ktx.common

fun ByteArray.chunked(count: Int): List<ByteArray> {
    return asIterable()
        .chunked(count)
        .map(Collection<Byte>::toByteArray)
}