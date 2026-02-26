package net.flipper.bridge.device.firmwareupdate.downloader.util

import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.SHA256
import kotlinx.io.RawSource
import kotlinx.io.bytestring.toHexString
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

suspend fun Path.sha256() = SystemFileSystem.source(this).sha256()
suspend fun RawSource.sha256(): String {
    val provider = CryptographyProvider.Default
    val hasher = provider.get(SHA256).hasher()
    val hash = hasher.hash(this)
    return hash.toHexString()
}
