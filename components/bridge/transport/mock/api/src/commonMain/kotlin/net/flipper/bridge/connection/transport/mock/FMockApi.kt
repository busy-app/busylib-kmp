package net.flipper.bridge.connection.transport.mock

import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.bridge.connection.transport.common.api.meta.FTransportMetaInfoApi
import net.flipper.bridge.connection.transport.common.api.serial.FHTTPDeviceApi

interface FMockApi : FConnectedDeviceApi, FHTTPDeviceApi, FTransportMetaInfoApi
