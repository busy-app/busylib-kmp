package com.flipperdevices.core.ktx.common

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlin.time.Duration

/**
 * [TickFlow] is used to tick every [duration]
 */
class TickFlow(
    duration: Duration,
    initialDelay: Duration = Duration.ZERO
) : Flow<Unit> by flow(
    block = {
        delay(initialDelay)
        while (currentCoroutineContext().isActive) {
            emit(Unit)
            delay(duration)
        }
    }
)
