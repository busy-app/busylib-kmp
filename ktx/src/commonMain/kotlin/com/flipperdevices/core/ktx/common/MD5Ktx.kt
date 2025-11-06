package com.flipperdevices.core.ktx.common

import kotlinx.io.Source
import org.kotlincrypto.hash.md.MD5

private const val BYTE_BUFFER_SIZE = 512

suspend fun Source.md5(): Result<String> = runSuspendCatching {
    val digest = MD5()

    val buffer = ByteArray(BYTE_BUFFER_SIZE)
    var readBytes = readAtMostTo(buffer)
    while (readBytes != -1) {
        digest.update(buffer, offset = 0, len = readBytes)
        readBytes = readAtMostTo(buffer)
    }

    val md5Bytes = digest.digest()
    return@runSuspendCatching md5Bytes.toHexString(
        HexFormat {
            upperCase = false
        }
    )
}
