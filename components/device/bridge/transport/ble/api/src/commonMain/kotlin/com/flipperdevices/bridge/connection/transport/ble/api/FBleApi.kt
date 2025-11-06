package com.flipperdevices.bridge.connection.transport.ble.api

import com.flipperdevices.bridge.connection.transport.common.api.FConnectedDeviceApi
import com.flipperdevices.bridge.connection.transport.common.api.meta.FTransportMetaInfoApi
import com.flipperdevices.bridge.connection.transport.common.api.serial.FHTTPDeviceApi

interface FBleApi : FHTTPDeviceApi, FConnectedDeviceApi, FTransportMetaInfoApi
