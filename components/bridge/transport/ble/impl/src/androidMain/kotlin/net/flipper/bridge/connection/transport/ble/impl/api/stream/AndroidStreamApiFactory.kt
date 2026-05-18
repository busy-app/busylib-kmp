package net.flipper.bridge.connection.transport.ble.impl.api.stream

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import net.flipper.bridge.connection.transport.ble.api.FBleDeviceStatusStreamingConfig
import net.flipper.bridge.connection.transport.ble.impl.api.utils.isNotifyAvailable
import net.flipper.bridge.connection.transport.ble.impl.streaming.FBleStatusStreamingApiImpl
import net.flipper.bridge.connection.transport.common.api.serial.FStatusStreamingApi
import net.flipper.busylib.core.wrapper.WrappedStateFlow
import net.flipper.core.busylib.log.TaggedLogger
import net.flipper.core.busylib.log.error
import net.flipper.core.busylib.log.info
import no.nordicsemi.kotlin.ble.client.RemoteService

object AndroidStreamApiFactory {
    fun buildStreamingApi(
        config: FBleDeviceStatusStreamingConfig,
        services: StateFlow<List<RemoteService>>,
        scope: CoroutineScope
    ): FStatusStreamingApi {
        val serialService = services.map { services ->
            services.find { it.uuid == config.serviceUuid }
        }
        val streamingChar = serialService.map { service ->
            service?.characteristics?.find { it.uuid == config.notifyCharUuid }
        }
        val logger = TaggedLogger("AndroidStreamApi")
        val byteFlow = streamingChar
            .filterNotNull()
            .onEach {
                if (it.isNotifyAvailable().not()) {
                    logger.error { "Found char ${it.uuid}, but notify is disabled" }
                }
            }
            .filter {
                it.isNotifyAvailable()
            }
            .flatMapLatest {
                logger.info { "Start subscribe on streaming char" }
                val flow = it.subscribe()
                return@flatMapLatest flow
            }
        return FBleStatusStreamingApiImpl(
            subscribeFlow = byteFlow,
            scope = scope
        )
    }
}
