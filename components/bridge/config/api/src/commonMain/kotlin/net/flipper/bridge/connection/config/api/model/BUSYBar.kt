package net.flipper.bridge.connection.config.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.flipper.bridge.connection.config.api.model.BUSYBar.ConnectionWay
import net.flipper.core.busylib.data.NonEmptyList
import net.flipper.core.busylib.data.nonEmptyListOf
import kotlin.uuid.Uuid

@Serializable
class BUSYBar internal constructor(
    @SerialName("human_readable_name")
    val humanReadableName: String,
    @SerialName("hardware_id")
    val hardwareId: String? = null, // default for serialization
    @SerialName("on_call_enabled")
    val onCallEnabled: Boolean? = null, // default for serialization
    @SerialName("unique_id")
    val uniqueId: String,
    @SerialName("connection_way_ble")
    val ble: ConnectionWay.BLE?,
    @SerialName("connection_way_cloud")
    val cloud: ConnectionWay.Cloud?,
    @SerialName("connection_way_lan")
    val lan: ConnectionWay.Lan?,
    @SerialName("connection_way_mock")
    val mock: ConnectionWay.Mock?,
    /**
     * Returns all non-null connection ways ordered by priority (highest first):
     * Lan(100) > Cloud(10) > BLE(0) > Mock(-1)
     */
    @Transient
    val connectionWays: NonEmptyList<ConnectionWay> =
        listOfNotNull(lan, cloud, ble, mock).let { list ->
            require(list.isNotEmpty()) {
                "Invalid BUSYBar '$uniqueId': at least one connection way must be present"
            }
            val first = list.first() // Can be crashed on deserialization
            return@let nonEmptyListOf(first, list.drop(1))
        }
) {

    @Serializable
    sealed interface ConnectionWay {
        @Serializable
        data class BLE(
            @SerialName("address")
            val address: String
        ) : ConnectionWay

        @Serializable
        data object Mock : ConnectionWay

        @Serializable
        data class Lan(
            @SerialName("host")
            val host: String = "10.0.4.20",
        ) : ConnectionWay

        @Serializable
        data class Cloud(
            @SerialName("device_id")
            val deviceId: Uuid
        ) : ConnectionWay
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as BUSYBar

        if (humanReadableName != other.humanReadableName) return false
        if (hardwareId != other.hardwareId) return false
        if (uniqueId != other.uniqueId) return false
        if (ble != other.ble) return false
        if (cloud != other.cloud) return false
        if (lan != other.lan) return false
        if (mock != other.mock) return false
        if (onCallEnabled != other.onCallEnabled) return false
        if (connectionWays != other.connectionWays) return false

        return true
    }

    override fun hashCode(): Int {
        var result = humanReadableName.hashCode()
        result = 31 * result + (hardwareId?.hashCode() ?: 0)
        result = 31 * result + uniqueId.hashCode()
        result = 31 * result + (ble?.hashCode() ?: 0)
        result = 31 * result + (cloud?.hashCode() ?: 0)
        result = 31 * result + (lan?.hashCode() ?: 0)
        result = 31 * result + (mock?.hashCode() ?: 0)
        result = 31 * result + onCallEnabled.hashCode()
        result = 31 * result + connectionWays.hashCode()
        return result
    }

    @Suppress("MaximumLineLength", "MaxLineLength")
    override fun toString(): String {
        return "BUSYBar(humanReadableName='$humanReadableName', hardwareId=$hardwareId, uniqueId='$uniqueId', ble=$ble, cloud=$cloud, lan=$lan, mock=$mock, onCallEnabled=$onCallEnabled, connectionWays=$connectionWays)"
    }
}
