package net.flipper.bridge.connection.feature.events.api

import kotlin.time.TimeSource

data class UpdateEventData(
    val event: UpdateEvent
) {
    val sentAt: TimeSource.Monotonic.ValueTimeMark = TimeSource.Monotonic.markNow()
}
