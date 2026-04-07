package net.flipper.bridge.connection.feature.events.model

import net.flipper.bridge.connection.feature.rpc.api.model.BsbBrightnessInfo
import net.flipper.bridge.connection.feature.rpc.api.model.Fraction

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
        val isConnected: Boolean,
        val ssid: String?,
    ) : BusyLibUpdateEvent

    sealed interface Update : BusyLibUpdateEvent {
        data class UpdateState(
            val action: Action,
            val status: Status
        ) : Update {

            enum class Action {
                DOWNLOAD, SHA_VERIFICATION, UNPACK, PREPARE, APPLY, NONE
            }

            enum class Status {
                OK, BATTERY_LOW, BUSY,
                DOWNLOAD_FAILURE, DOWNLOAD_ABORT, SHA_MISMATCH,
                UNPACK_STAGING_DIR_FAILURE, UNPACK_ARCHIVE_OPEN_FAILURE, UNPACK_ARCHIVE_UNPACK_FAILURE,
                INSTALL_MANIFEST_NOT_FOUND, INSTALL_MANIFEST_INVALID,
                INSTALL_SESSION_CONFIG_FAILURE, INSTALL_POINTER_SETUP_FAILURE,
                UNKNOWN_FAILURE
            }

            enum class CheckResult {
                AVAILABLE, NOT_AVAILABLE, FAILURE, NONE
            }
        }

        data class UpdateCheck(
            val availableVersion: String?,
        ) : Update

        data class UpdateStatus(val status: net.flipper.bridge.connection.feature.rpc.api.model.UpdateStatus) : Update
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
