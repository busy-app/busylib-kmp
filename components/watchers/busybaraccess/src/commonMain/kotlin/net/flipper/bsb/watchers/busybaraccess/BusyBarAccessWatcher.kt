package net.flipper.bsb.watchers.busybaraccess

import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import net.flipper.bsb.cloud.barsws.api.CloudWebSocketApi
import net.flipper.bsb.cloud.barsws.api.WebSocketEvent
import net.flipper.bsb.watchers.api.InternalBUSYLibStartupListener
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.core.busylib.ktx.common.SingleJobMode
import net.flipper.core.busylib.ktx.common.asSingleJobScope
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.info
import net.flipper.eventbus.internal.BusyLibEventPublisher
import net.flipper.eventbus.model.BusyLibEvent

@Inject
@ContributesIntoSet(BusyLibGraph::class, binding<InternalBUSYLibStartupListener>())
class BusyBarAccessWatcher(
    scope: CoroutineScope,
    private val webSocketApi: CloudWebSocketApi,
    private val eventPublisher: BusyLibEventPublisher
) : InternalBUSYLibStartupListener, LogTagProvider {
    override val TAG = "BusyBarAccessWatcher"

    private val singleJobScope = scope.asSingleJobScope()

    override fun onLaunch() {
        info { "Launched" }
        singleJobScope.launch(SingleJobMode.CANCEL_PREVIOUS) {
            webSocketApi.getWSFlow()
                .flatMapLatest { webSocket -> webSocket?.getEventsFlow() ?: emptyFlow() }
                .filterIsInstance<WebSocketEvent.BusyBarAccessUpdatedEvent>()
                .collect { event ->
                    info { "Receive $event" }
                    eventPublisher.publish(
                        BusyLibEvent.BusyBarAccessUpdated(
                            granted = event.granted,
                            graceUntil = event.graceUntil
                        )
                    )
                }
        }
    }
}
