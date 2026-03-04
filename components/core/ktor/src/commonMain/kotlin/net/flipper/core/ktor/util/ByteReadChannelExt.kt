package net.flipper.core.ktor.util

import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.asSource
import kotlinx.coroutines.flow.Flow

@Suppress("MagicNumber")
fun ByteReadChannel.asFlow(
    bufferSize: Long = 1 * 1024
): Flow<ByteArray> = asSource().asFlow(bufferSize)
