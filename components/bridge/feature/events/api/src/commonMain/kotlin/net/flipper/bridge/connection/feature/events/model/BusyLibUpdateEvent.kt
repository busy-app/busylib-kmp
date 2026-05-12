package net.flipper.bridge.connection.feature.events.model

import kotlinx.datetime.UtcOffset
import net.flipper.bridge.connection.feature.settings.model.BsbBrightnessInfo
import net.flipper.core.busylib.data.Fraction

/**
 * This update events consumed/received only through BusyLib
 */
sealed interface BusyLibUpdateEvent {
    data class Brightness(val bsbBrightnessInfo: BsbBrightnessInfo) : BusyLibUpdateEvent

    data class Volume(val volume: Fraction) : BusyLibUpdateEvent
    data class DeviceName(val deviceName: String) : BusyLibUpdateEvent
    data class AutoUpdateChanged(val isEnabled: Boolean) : BusyLibUpdateEvent

    sealed interface Power : BusyLibUpdateEvent {
        data class Provided(
            val status: Status?,
            val chargePercent: Fraction,
        ) : Power {
            enum class Status {
                DISCHARGING,
                CHARGING,
                CHARGED
            }
        }

        data object Unknown : Power
    }

    data class Wifi(
        val ips: List<IpAddress>,
        val state: State
    ) : BusyLibUpdateEvent {
        sealed interface State {
            data object Unknown : State
            data object Disconnected : State
            data class Connected(
                val ssid: String,
                val bssid: String,
                val channel: Int,
                val rssi: Int,
                val security: Security,
                val status: Status
            ) : State {
                enum class Status {
                    CONNECTED,
                    CONNECTING,
                    DISCONNECTING,
                    RECONNECTING
                }

                enum class Security {
                    UNKNOWN,
                    OPEN,
                    WPA,
                    WPA2,
                    WEP,
                    WPA_WPA2,
                    WPA3,
                    WPA2_WPA3
                }
            }
        }

        data class IpAddress(
            val protocol: IpProtocol,
            val method: IpConfigurationMethod,
            val address: String,
            val gateway: String,
            val netmask: String
        ) {
            enum class IpProtocol {
                IPV4,
                IPV6
            }

            enum class IpConfigurationMethod {
                DHCP,
                STATIC
            }
        }
    }

    sealed interface Update : BusyLibUpdateEvent {
        data class UpdateState(
            val action: BsbAction,
            val status: BsbStatus,
        ) : Update {
            enum class BsbAction {
                DOWNLOAD,
                SHA_VERIFICATION,
                UNPACK,
                PREPARE,
                APPLY,
                NONE
            }

            enum class BsbStatus {
                OK,
                BATTERY_LOW,
                BUSY,
                DOWNLOAD_FAILURE,
                DOWNLOAD_ABORT,
                SHA_MISMATCH,
                UNPACK_STAGING_DIR_FAILURE,
                UNPACK_ARCHIVE_OPEN_FAILURE,
                UNPACK_ARCHIVE_UNPACK_FAILURE,
                INSTALL_MANIFEST_NOT_FOUND,
                INSTALL_MANIFEST_INVALID,
                INSTALL_SESSION_CONFIG_FAILURE,
                INSTALL_POINTER_SETUP_FAILURE,
                UNKNOWN_FAILURE
            }
        }

        data class UpdateCheck(
            val result: CheckResult,
            val event: CheckEvent
        ) : Update {
            enum class CheckEvent {
                START,
                STOP,
                NONE
            }

            sealed interface CheckResult {
                data class Available(
                    val availableVersion: String
                ) : CheckResult

                data class Unavailable(
                    val reason: CheckError
                ) : CheckResult {
                    enum class CheckError {
                        NOT_AVAILABLE,
                        FAILURE,
                        IDLE
                    }
                }
            }
        }
    }

    data class Timezone(
        val name: String,
        val offset: UtcOffset,
        val abbreviation: String,
    ) : BusyLibUpdateEvent

    data class Ble(
        val status: BleServiceStatus,
        val remoteAddress: String?,
    ) : BusyLibUpdateEvent {
        enum class BleServiceStatus {
            RESET,
            INITIALIZATION,
            READY,
            ADVERTISING,
            CONNECTABLE,
            CONNECTED,
            ERROR,
        }
    }

    data class Matter(
        val fabricCount: Int,
    ) : BusyLibUpdateEvent

    data class Frame(
        val screen: Screen,
        val width: Int,
        val height: Int,
        val encoding: Encoding,
        val pixelFormat: PixelFormat,
        val data: ByteArray,
    ) : BusyLibUpdateEvent {
        enum class Screen {
            FRONT,
            BACK,
        }

        enum class Encoding {
            PLAIN,
            RUN_LENGTH,
            DEFLATE,
            DEFLATE_RUN_LENGTH,
        }

        enum class PixelFormat {
            RGB888,
            L8,
            L4,
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as Frame

            if (screen != other.screen) return false
            if (width != other.width) return false
            if (height != other.height) return false
            if (encoding != other.encoding) return false
            if (pixelFormat != other.pixelFormat) return false
            if (!data.contentEquals(other.data)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = screen.hashCode()
            result = 31 * result + width
            result = 31 * result + height
            result = 31 * result + encoding.hashCode()
            result = 31 * result + pixelFormat.hashCode()
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

    data class Profiles(
        val byName: Map<String, String>,
    ) : BusyLibUpdateEvent
}
