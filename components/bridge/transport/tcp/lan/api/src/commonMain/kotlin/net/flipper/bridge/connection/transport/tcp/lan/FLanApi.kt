package net.flipper.bridge.connection.transport.tcp.lan

import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.bridge.connection.transport.common.api.serial.FHTTPDeviceApi
import net.flipper.bridge.connection.transport.common.api.serial.FStatusStreamingApi

interface FLanApi : FConnectedDeviceApi, FHTTPDeviceApi, FStatusStreamingApi
