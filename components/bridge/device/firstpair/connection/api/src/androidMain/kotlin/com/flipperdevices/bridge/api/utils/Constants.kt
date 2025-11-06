package com.flipperdevices.bridge.api.utils

import no.nordicsemi.kotlin.ble.core.util.fromShortUuid
import kotlin.uuid.Uuid

object Constants {
    const val UNKNOWN_NAME = "BUSY Bar"

    // BLE information service uuids: service uuid and characteristics uuids
    object BLEInformationService {
        const val COMPANY_ID = 0x0E29
        val SERVICE_UUID: Uuid = Uuid.fromShortUuid(shortUuid = 0x308A)
    }
}
