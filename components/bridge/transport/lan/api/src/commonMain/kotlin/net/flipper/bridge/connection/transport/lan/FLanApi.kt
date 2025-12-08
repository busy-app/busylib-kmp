package net.flipper.bridge.connection.transport.lan

import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.bridge.connection.transport.common.api.serial.FHTTPDeviceApi

interface FLanApi : FConnectedDeviceApi, FHTTPDeviceApi
