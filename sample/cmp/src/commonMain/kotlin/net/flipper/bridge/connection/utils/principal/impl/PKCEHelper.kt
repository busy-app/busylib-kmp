package net.flipper.bridge.connection.utils.principal.impl

import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.SHA256
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.random.Random

internal object PKCEHelper {
    @OptIn(ExperimentalEncodingApi::class)
    fun generateCodeVerifier(): String {
        val bytes = Random.nextBytes(32)
        return Base64.UrlSafe.encode(bytes).trimEnd('=')
    }

    @OptIn(ExperimentalEncodingApi::class)
    suspend fun generateCodeChallenge(codeVerifier: String): String {
        val provider = CryptographyProvider.Default
        val hasher = provider.get(SHA256).hasher()
        val hash = hasher.hash(codeVerifier.encodeToByteArray())
        return Base64.UrlSafe.encode(hash).trimEnd('=')
    }
}
