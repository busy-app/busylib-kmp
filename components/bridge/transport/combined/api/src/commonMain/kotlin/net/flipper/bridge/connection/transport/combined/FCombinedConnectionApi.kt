package net.flipper.bridge.connection.transport.combined

import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.bridge.connection.transport.common.api.serial.FHTTPDeviceApi

interface FCombinedConnectionApi : FConnectedDeviceApi, FHTTPDeviceApi
