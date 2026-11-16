package net.flipper.busylib

import net.flipper.bridge.connection.feature.screenstreaming.model.BusyImageFormat
import net.flipper.core.busylib.ktx.common.toNSData
import platform.Foundation.NSData

fun BusyImageFormat.toNSData(): NSData = byteArray.toNSData()
