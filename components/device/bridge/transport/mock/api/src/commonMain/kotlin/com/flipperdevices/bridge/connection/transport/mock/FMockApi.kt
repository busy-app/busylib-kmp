package com.flipperdevices.bridge.connection.transport.mock

import com.flipperdevices.bridge.connection.transport.common.api.FConnectedDeviceApi
import com.flipperdevices.bridge.connection.transport.common.api.meta.FTransportMetaInfoApi
import com.flipperdevices.bridge.connection.transport.common.api.serial.FHTTPDeviceApi

interface FMockApi : FConnectedDeviceApi, FHTTPDeviceApi, FTransportMetaInfoApi
