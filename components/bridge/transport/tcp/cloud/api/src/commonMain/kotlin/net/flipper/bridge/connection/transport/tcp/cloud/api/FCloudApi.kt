package net.flipper.bridge.connection.transport.tcp.cloud.api

import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.bridge.connection.transport.common.api.serial.FHTTPDeviceApi

interface FCloudApi : FConnectedDeviceApi, FHTTPDeviceApi
