package net.flipper.tools.oncall.impl.session

import dev.zacsweers.metro.Inject
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.http.isSuccess
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeoutOrNull
import net.flipper.bridge.lanmonitor.api.BB_PORT
import net.flipper.core.busylib.ktx.common.TickFlow
import net.flipper.core.busylib.ktx.common.runSuspendCatching
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.info
import net.flipper.core.ktor.di.qualifier.KtorNetworkClientQualifier
import kotlin.time.Duration.Companion.seconds

@Inject
class LanOnCallDeviceScanner(
    @KtorNetworkClientQualifier
    private val httpClient: HttpClient
) : LogTagProvider {
    override val TAG: String = "LanOnCallDeviceScanner"

    private fun getHostAt(index: Int): String {
        val offset = THIRD_OCTET_START * OCTET_SIZE + FOURTH_OCTET_START + index
        val thirdOctet = offset / OCTET_SIZE
        val fourthOctet = offset % OCTET_SIZE
        return "10.0.$thirdOctet.$fourthOctet"
    }

    private suspend fun isHostReachable(host: String): Boolean {
        return withTimeoutOrNull(PROBE_TIMEOUT) {
            runSuspendCatching {
                httpClient
                    .get("http://$host:$BB_PORT/api/status/device")
                    .status
                    .isSuccess()
            }.getOrDefault(false)
        } ?: false
    }

    private suspend fun scanOnce(): Set<String> = coroutineScope {
        val hosts = mutableSetOf<String>()
        var index = 0
        var consecutiveMisses = 0
        while (consecutiveMisses < CONSECUTIVE_MISS_LIMIT && index < MAX_SCANNED_HOSTS) {
            val batch = List(PROBE_BATCH_SIZE) { offset -> getHostAt(index + offset) }
            index += PROBE_BATCH_SIZE
            batch
                .map { host -> async { if (isHostReachable(host)) host else null } }
                .awaitAll()
                .forEach { host ->
                    if (host == null) {
                        consecutiveMisses++
                    } else {
                        consecutiveMisses = 0
                        hosts += host
                    }
                }
        }
        if (hosts.isNotEmpty()) {
            info { "Found LAN devices: $hosts" }
        }
        return@coroutineScope hosts
    }

    fun getLanHostsFlow(): Flow<Set<String>> {
        return TickFlow(SCAN_INTERVAL)
            .map { _ -> scanOnce() }
            .distinctUntilChanged()
    }

    companion object {
        private const val OCTET_SIZE = 256

        /**
         * 10.0.4.20 — the address of the first bar on the LAN link
         * @see net.flipper.bridge.lanmonitor.api.BB_HOST
         */
        private const val THIRD_OCTET_START = 4
        private const val FOURTH_OCTET_START = 20

        private const val PROBE_BATCH_SIZE = 4
        private const val CONSECUTIVE_MISS_LIMIT = 4
        private const val MAX_SCANNED_HOSTS = 64
        private val PROBE_TIMEOUT = 2.seconds
        private val SCAN_INTERVAL = 10.seconds
    }
}
