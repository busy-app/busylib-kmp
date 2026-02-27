package net.flipper.bsb.cloud.rest.channel.krate

import com.russhwolf.settings.Settings
import me.tatarka.inject.annotations.Inject
import net.flipper.bsb.cloud.rest.model.BsbFirmwareChannelId
import ru.astrainteractive.klibs.kstorage.api.StateFlowMutableKrate
import ru.astrainteractive.klibs.kstorage.api.impl.DefaultMutableKrate
import ru.astrainteractive.klibs.kstorage.util.asStateFlowMutableKrate

private const val BSB_FIRMWARE_CHANGELOG_ID_KEY = "bsb_firmware_channel_id_key"

@Inject
class BsbFirmwareChannelIdKrate(
    private val settings: Settings
) : StateFlowMutableKrate<BsbFirmwareChannelId> by DefaultMutableKrate(
    factory = { BsbFirmwareChannelId.DEVELOPMENT },
    loader = {
        val string = settings.getStringOrNull(BSB_FIRMWARE_CHANGELOG_ID_KEY)
        BsbFirmwareChannelId.entries
            .firstOrNull { entry -> entry.id == string }
            ?: BsbFirmwareChannelId.DEVELOPMENT
    },
    saver = { bsbFirmwareChannelId ->
        settings.putString(bsbFirmwareChannelId.id, BSB_FIRMWARE_CHANGELOG_ID_KEY)
    }
).asStateFlowMutableKrate()
