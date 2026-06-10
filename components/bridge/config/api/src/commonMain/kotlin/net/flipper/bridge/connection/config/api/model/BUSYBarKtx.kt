package net.flipper.bridge.connection.config.api.model

import net.flipper.bridge.connection.config.api.model.BUSYBar.ConnectionWay
import kotlin.uuid.Uuid

fun BUSYBar(
    humanReadableName: String,
    uniqueId: String = Uuid.random().toString(),
    ble: ConnectionWay.BLE
): BUSYBar {
    return BUSYBar(
        humanReadableName = humanReadableName,
        hardwareId = null,
        uniqueId = uniqueId,
        ble = ble,
        cloud = null,
        lan = null,
        mock = null,
        onCallEnabled = null,
    )
}

fun BUSYBar(
    humanReadableName: String,
    hardwareId: String? = null,
    uniqueId: String = Uuid.random().toString(),
    cloud: ConnectionWay.Cloud
): BUSYBar {
    return BUSYBar(
        humanReadableName = humanReadableName,
        uniqueId = uniqueId,
        hardwareId = hardwareId,
        ble = null,
        cloud = cloud,
        lan = null,
        mock = null,
        onCallEnabled = null,
    )
}

fun BUSYBar(
    humanReadableName: String,
    hardwareId: String,
    uniqueId: String = Uuid.random().toString(),
    lan: ConnectionWay.Lan
): BUSYBar {
    return BUSYBar(
        humanReadableName = humanReadableName,
        hardwareId = hardwareId,
        uniqueId = uniqueId,
        ble = null,
        cloud = null,
        lan = lan,
        mock = null,
        onCallEnabled = null,
    )
}

fun BUSYBar(
    humanReadableName: String,
    hardwareId: String? = null,
    uniqueId: String = Uuid.random().toString(),
    onCallEnabled: Boolean? = null,
    mock: ConnectionWay.Mock
): BUSYBar {
    return BUSYBar(
        humanReadableName = humanReadableName,
        hardwareId = hardwareId,
        uniqueId = uniqueId,
        ble = null,
        cloud = null,
        lan = null,
        mock = mock,
        onCallEnabled = onCallEnabled,
    )
}

fun BUSYBar.copy(
    humanReadableName: String = this.humanReadableName,
    hardwareId: String? = this.hardwareId,
    onCallEnabled: Boolean? = this.onCallEnabled
): BUSYBar {
    return BUSYBar(
        humanReadableName = humanReadableName,
        hardwareId = hardwareId,
        uniqueId = uniqueId,
        ble = ble,
        cloud = cloud,
        lan = lan,
        mock = mock,
        onCallEnabled = onCallEnabled,
    )
}

fun BUSYBar.copyTransports(
    uniqueId: String
): BUSYBar {
    return BUSYBar(
        humanReadableName = humanReadableName,
        uniqueId = uniqueId,
        hardwareId = hardwareId,
        ble = ble,
        cloud = cloud,
        lan = lan,
        mock = mock,
        onCallEnabled = onCallEnabled,
    )
}

fun BUSYBar.copy(
    uniqueId: String = this.uniqueId,
    ble: ConnectionWay.BLE? = this.ble,
    cloud: ConnectionWay.Cloud? = this.cloud,
    lan: ConnectionWay.Lan? = this.lan,
    mock: ConnectionWay.Mock? = this.mock,
    onCallEnabled: Boolean? = this.onCallEnabled,
): BUSYBar? {
    if (listOfNotNull(lan, cloud, ble, mock).isEmpty()) {
        return null
    }
    return BUSYBar(
        humanReadableName = humanReadableName,
        hardwareId = hardwareId,
        uniqueId = uniqueId,
        ble = ble,
        cloud = cloud,
        lan = lan,
        mock = mock,
        onCallEnabled = onCallEnabled,
    )
}

fun BUSYBar.addTransport(
    ble: ConnectionWay.BLE? = null,
    cloud: ConnectionWay.Cloud? = null,
    lan: ConnectionWay.Lan? = null,
    mock: ConnectionWay.Mock? = null,
): BUSYBar {
    return BUSYBar(
        humanReadableName = humanReadableName,
        hardwareId = hardwareId,
        uniqueId = uniqueId,
        ble = ble ?: this.ble,
        cloud = cloud ?: this.cloud,
        lan = lan ?: this.lan,
        mock = mock ?: this.mock,
        onCallEnabled = onCallEnabled,
    )
}

fun mergeBBIfEmpty(original: BUSYBar, other: BUSYBar): BUSYBar {
    var result = original
    if (result.ble == null) {
        result = result.addTransport(ble = other.ble)
    }
    if (result.cloud == null) {
        result = result.addTransport(cloud = other.cloud)
    }
    if (result.lan == null) {
        result = result.addTransport(lan = other.lan)
    }
    if (result.hardwareId == null) {
        result = result.copy(hardwareId = other.hardwareId)
    }
    if (result.onCallEnabled == null) {
        result = result.copy(onCallEnabled = other.onCallEnabled)
    }
    // Ignore mock just in case
    return result
}
