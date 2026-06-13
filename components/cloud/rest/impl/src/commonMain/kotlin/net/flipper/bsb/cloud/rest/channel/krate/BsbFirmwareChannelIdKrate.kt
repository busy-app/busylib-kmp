package net.flipper.bsb.cloud.rest.channel.krate

import com.russhwolf.settings.Settings
import dev.zacsweers.metro.Inject
import net.flipper.bsb.cloud.rest.model.BsbFirmwareChannelId
import net.flipper.busylib.kmp.components.core.buildkonfig.BuildKonfig
import ru.astrainteractive.klibs.kstorage.api.StateFlowMutableKrate
import ru.astrainteractive.klibs.kstorage.api.asStateFlowMutableKrate
import ru.astrainteractive.klibs.kstorage.api.impl.DefaultMutableKrate

private val BSB_FIRMWARE_CHANGELOG_ID_KEY: String
    get() = when (BuildKonfig.IS_DEVELOP_FIRMWARE_CHANNEL) {
        true -> "bsb_firmware_channel_id_key_dev"
        else -> "bsb_firmware_channel_id_key_v2"
    }

private val BsbFirmwareChannelId.Companion.DEFAULT: BsbFirmwareChannelId
    get() = when (BuildKonfig.IS_DEVELOP_FIRMWARE_CHANNEL) {
        true -> BsbFirmwareChannelId.DEVELOPMENT
        else -> BsbFirmwareChannelId.RELEASE
    }

@Inject
class BsbFirmwareChannelIdKrate(
    private val settings: Settings
) : StateFlowMutableKrate<BsbFirmwareChannelId> by DefaultMutableKrate(
    factory = { BsbFirmwareChannelId.DEFAULT },
    loader = {
        val string = settings.getStringOrNull(BSB_FIRMWARE_CHANGELOG_ID_KEY)
        BsbFirmwareChannelId.entries
            .firstOrNull { entry -> entry.id == string }
            ?: BsbFirmwareChannelId.DEFAULT
    },
    saver = { bsbFirmwareChannelId ->
        settings.putString(
            key = BSB_FIRMWARE_CHANGELOG_ID_KEY,
            value = bsbFirmwareChannelId.id
        )
    }
).asStateFlowMutableKrate()
