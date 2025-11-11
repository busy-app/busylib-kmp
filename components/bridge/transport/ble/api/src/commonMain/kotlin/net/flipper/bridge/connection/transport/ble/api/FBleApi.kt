package net.flipper.bridge.connection.transport.ble.api

import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.bridge.connection.transport.common.api.meta.FTransportMetaInfoApi
import net.flipper.bridge.connection.transport.common.api.serial.FHTTPDeviceApi

interface FBleApi : FHTTPDeviceApi, FConnectedDeviceApi, FTransportMetaInfoApi
