package net.flipper.tools.drawtool.sync.trigger

import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import net.flipper.busylib.core.di.BusyLibGraph

/** Reports a change of the local collection made through the library API. */
interface DrawToolCollectionEventProducer {
    fun notifyChanged()
}

/**
 * Delivers what [DrawToolCollectionEventProducer] reported. Changes made
 * behind the library's back (the host app writing status files itself) never
 * arrive here.
 */
interface DrawToolCollectionEventConsumer {
    val events: Flow<Unit>
}

@SingleIn(BusyLibGraph::class)
@Inject
@ContributesBinding(BusyLibGraph::class, binding<DrawToolCollectionEventProducer>())
@ContributesBinding(BusyLibGraph::class, binding<DrawToolCollectionEventConsumer>())
class DrawToolCollectionEvents :
    DrawToolCollectionEventProducer,
    DrawToolCollectionEventConsumer {

    private val mutableEvents = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    override val events: Flow<Unit> = mutableEvents

    override fun notifyChanged() {
        mutableEvents.tryEmit(Unit)
    }
}
