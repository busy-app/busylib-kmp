package net.flipper.tools.drawtool.sync.storage

import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.coroutines.toFlowSettings
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.core.busylib.data.di.qualifier.BusyLibJsonQualifier
import net.flipper.core.busylib.ktx.common.runSuspendCatching
import net.flipper.core.busylib.log.TaggedLogger
import net.flipper.core.busylib.log.error
import net.flipper.tools.drawtool.sync.model.DrawToolSyncStateSnapshot
import ru.astrainteractive.klibs.kstorage.suspend.FlowMutableKrate
import ru.astrainteractive.klibs.kstorage.suspend.impl.DefaultFlowMutableKrate

interface DrawToolSyncStateKrate : FlowMutableKrate<DrawToolSyncStateSnapshot>

/** An unreadable stored value degrades to the empty snapshot. */
@OptIn(ExperimentalSettingsApi::class)
@SingleIn(BusyLibGraph::class)
@Inject
@ContributesBinding(BusyLibGraph::class, binding<DrawToolSyncStateKrate>())
class DrawToolSyncStateKrateImpl(
    observableSettings: ObservableSettings,
    @BusyLibJsonQualifier json: Json,
) : DrawToolSyncStateKrate,
    FlowMutableKrate<DrawToolSyncStateSnapshot> by DefaultFlowMutableKrate(
        factory = { DrawToolSyncStateSnapshot() },
        loader = {
            observableSettings
                .toFlowSettings()
                .getStringOrNullFlow(KEY)
                .map { stringValue ->
                    if (stringValue.isNullOrBlank()) {
                        null
                    } else {
                        runSuspendCatching { json.decodeFromString(Serializer, stringValue) }
                            .getOrElse { throwable ->
                                logger.error(throwable) {
                                    "Could not read the sync state, starting empty"
                                }
                                null
                            }
                    }
                }
        },
        saver = { snapshot ->
            observableSettings.putString(KEY, json.encodeToString(Serializer, snapshot))
        }
    ) {
    companion object {
        private const val KEY = "draw_tool_sync_state"
        private val Serializer get() = DrawToolSyncStateSnapshot.serializer()
        private val logger = TaggedLogger("DrawToolSyncStateKrate")
    }
}
