package com.flipperdevices.busylib.di

import com.flipperdevices.bridge.connection.ble.impl.FDeviceHolderFactory

interface FDeviceHolderFactoryModule {
    val deviceHolderFactory: FDeviceHolderFactory
}