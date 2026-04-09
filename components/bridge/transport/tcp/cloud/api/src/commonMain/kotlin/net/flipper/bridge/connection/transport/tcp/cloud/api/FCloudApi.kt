package net.flipper.bridge.connection.transport.tcp.cloud.api

import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.bridge.connection.transport.common.api.serial.FHTTPDeviceApi
import net.flipper.bridge.connection.transport.common.api.serial.FStatusStreamingApi

interface FCloudApi : FConnectedDeviceApi, FHTTPDeviceApi, FStatusStreamingApi
