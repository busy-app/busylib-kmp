package net.flipper.bridge.connection.feature.events.model

import net.flipper.bridge.connection.feature.firmwareupdate.model.BsbUpdateStatus
import net.flipper.bridge.connection.feature.settings.model.BsbBrightnessInfo
import net.flipper.bridge.connection.feature.wifi.api.model.BsbWifiStatusResponse
import net.flipper.core.busylib.data.Fraction

/**
 * This update events consumed/received only through BusyLib
 */
sealed interface BusyLibUpdateEvent {
    data class Brightness(val bsbBrightnessInfo: BsbBrightnessInfo) : BusyLibUpdateEvent

    data class Volume(val volume: Fraction) : BusyLibUpdateEvent
    data class DeviceName(val deviceName: String) : BusyLibUpdateEvent
    data class AutoUpdateChanged(val isEnabled: Boolean) : BusyLibUpdateEvent

    data class Power(
        val batteryChargePercent: Fraction,
        val isCharging: Boolean,
    ) : BusyLibUpdateEvent

    data class Wifi(
        val state: BsbWifiStatusResponse.BsbWifiState,
        val ssid: String?,
        val bssid: String?,
        val channel: Int?,
        val rssi: Int?,
    ) : BusyLibUpdateEvent

    sealed interface Update : BusyLibUpdateEvent {
        data class UpdateState(
            val action: BsbUpdateStatus.BsbInstall.BsbAction,
            val status: BsbUpdateStatus.BsbInstall.BsbStatus
        ) : Update

        data class UpdateCheck(
            val availableVersion: String?,
        ) : Update

        data class BsbUpdateStatusChanged(
            val bsbUpdateStatus: BsbUpdateStatus
        ) : Update
    }

    data class Timezone(
        val name: String,
        val offsetMinutes: Int,
    ) : BusyLibUpdateEvent

    data class Matter(
        val fabricCount: Int,
    ) : BusyLibUpdateEvent

    data class Frame(
        val width: Int,
        val height: Int,
        val data: ByteArray,
    ) : BusyLibUpdateEvent {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as Frame

            if (width != other.width) return false
            if (height != other.height) return false
            if (!data.contentEquals(other.data)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = width
            result = 31 * result + height
            result = 31 * result + data.contentHashCode()
            return result
        }
    }

    sealed interface Input : BusyLibUpdateEvent {
        data class Button(val buttonName: String, val isPressed: Boolean) : Input
        data class Switch(val position: String) : Input
        data class Encoder(val delta: Int) : Input
    }

    data class Timer(
        val json: String,
    ) : BusyLibUpdateEvent
}
