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
        uniqueId = uniqueId,
        ble = ble,
        cloud = null,
        lan = null,
        mock = null,
    )
}

fun BUSYBar(
    humanReadableName: String,
    uniqueId: String = Uuid.random().toString(),
    cloud: ConnectionWay.Cloud
): BUSYBar {
    return BUSYBar(
        humanReadableName = humanReadableName,
        uniqueId = uniqueId,
        ble = null,
        cloud = cloud,
        lan = null,
        mock = null,
    )
}

fun BUSYBar(
    humanReadableName: String,
    uniqueId: String = Uuid.random().toString(),
    lan: ConnectionWay.Lan
): BUSYBar {
    return BUSYBar(
        humanReadableName = humanReadableName,
        uniqueId = uniqueId,
        ble = null,
        cloud = null,
        lan = lan,
        mock = null,
    )
}

fun BUSYBar(
    humanReadableName: String,
    uniqueId: String = Uuid.random().toString(),
    mock: ConnectionWay.Mock
): BUSYBar {
    return BUSYBar(
        humanReadableName = humanReadableName,
        uniqueId = uniqueId,
        ble = null,
        cloud = null,
        lan = null,
        mock = mock,
    )
}

fun BUSYBar.copy(
    humanReadableName: String = this.humanReadableName,
    hardwareId: String? = null
): BUSYBar {
    return BUSYBar(
        humanReadableName = humanReadableName,
        hardwareId = hardwareId,
        uniqueId = uniqueId,
        ble = ble,
        cloud = cloud,
        lan = lan,
        mock = mock
    )
}

fun BUSYBar.copyTransports(
    uniqueId: String
): BUSYBar {
    return BUSYBar(
        humanReadableName = humanReadableName,
        uniqueId = uniqueId,
        ble = ble,
        cloud = cloud,
        lan = lan,
        mock = mock
    )
}

fun BUSYBar.copy(
    uniqueId: String = this.uniqueId,
    ble: ConnectionWay.BLE? = this.ble,
    cloud: ConnectionWay.Cloud? = this.cloud,
    lan: ConnectionWay.Lan? = this.lan,
    mock: ConnectionWay.Mock? = this.mock,
): BUSYBar? {
    if (listOfNotNull(lan, cloud, ble, mock).isEmpty()) {
        return null
    }
    return BUSYBar(
        humanReadableName = humanReadableName,
        uniqueId = uniqueId,
        ble = ble,
        cloud = cloud,
        lan = lan,
        mock = mock
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
        uniqueId = uniqueId,
        ble = ble ?: this.ble,
        cloud = cloud ?: this.cloud,
        lan = lan ?: this.lan,
        mock = mock ?: this.mock
    )
}
