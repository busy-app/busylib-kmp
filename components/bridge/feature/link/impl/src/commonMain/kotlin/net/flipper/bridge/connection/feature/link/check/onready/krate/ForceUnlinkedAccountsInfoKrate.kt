package net.flipper.bridge.connection.feature.link.check.onready.krate

import com.russhwolf.settings.Settings
import kotlinx.serialization.json.Json
import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.feature.link.check.onready.model.ForceUnlinkedAccountInfo
import net.flipper.busylib.core.di.BusyLibGraph
import ru.astrainteractive.klibs.kstorage.api.StateFlowMutableKrate
import ru.astrainteractive.klibs.kstorage.api.impl.DefaultMutableKrate
import ru.astrainteractive.klibs.kstorage.util.asStateFlowMutableKrate
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding

private const val FORCED_UNLINKED_ACCOUNT_KEY = "dasdasd"

interface ForceUnlinkedAccountsInfoKrate : StateFlowMutableKrate<ForceUnlinkedAccountInfo>

@Inject
@ContributesBinding(BusyLibGraph::class, ForceUnlinkedAccountsInfoKrate::class)
class ForceUnlinkedAccountsInfoKrateImpl(
    private val settings: Settings,
    private val json: Json,
) : ForceUnlinkedAccountsInfoKrate,
    StateFlowMutableKrate<ForceUnlinkedAccountInfo> by DefaultMutableKrate(
        factory = { ForceUnlinkedAccountInfo() },
        loader = {
            val string = settings.getStringOrNull(FORCED_UNLINKED_ACCOUNT_KEY).orEmpty()
            runCatching { json.decodeFromString<ForceUnlinkedAccountInfo>(string) }
                .getOrNull()
        },
        saver = { forceUnlinkedAccountInfo ->
            val string = json.encodeToString(forceUnlinkedAccountInfo)
            settings.putString(FORCED_UNLINKED_ACCOUNT_KEY, string)
        }
    ).asStateFlowMutableKrate()
