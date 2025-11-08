package com.flipperdevices.busylib.di

import android.content.Context
import com.flipperdevices.bridge.connection.ble.impl.FDeviceHolderFactory
import com.flipperdevices.bridge.connection.connectionbuilder.impl.FDeviceConfigToConnectionImpl
import com.flipperdevices.bridge.connection.transport.ble.api.FBleDeviceConnectionConfig
import com.flipperdevices.bridge.connection.transport.mock.FMockDeviceConnectionConfig
import com.flipperdevices.bridge.connection.transport.mock.impl.BLEDeviceConnectionApiImpl
import com.flipperdevices.bridge.connection.transport.mock.impl.MockDeviceConnectionApiImpl
import com.flipperdevices.bridge.connection.transport.mock.impl.api.http.serial.FSerialUnsafeApiImpl
import com.flipperdevices.bridge.connection.transport.mock.impl.api.http.serial.SerialApiFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import no.nordicsemi.kotlin.ble.client.RemoteCharacteristic

class AndroidFDeviceHolderFactoryModule(
    scannerModule: ScannerModule,
    private val context: Context
) : FDeviceHolderFactoryModule {
    private fun createSerialApiFactory(): SerialApiFactory {
        return SerialApiFactory(
            unsafeApiImplFactory = object : FSerialUnsafeApiImpl.Factory {
                override fun invoke(
                    rxCharacteristic: Flow<RemoteCharacteristic?>,
                    txCharacteristic: Flow<RemoteCharacteristic?>,
                    scope: CoroutineScope
                ): FSerialUnsafeApiImpl {
                    return FSerialUnsafeApiImpl(
                        rxCharacteristic = rxCharacteristic,
                        txCharacteristic = txCharacteristic,
                        scope = scope,
                        context = context
                    )
                }
            }
        )
    }

    override val deviceHolderFactory: FDeviceHolderFactory = FDeviceHolderFactory(
        deviceConnectionHelper = FDeviceConfigToConnectionImpl(
            configToConnectionMap = mapOf(
                FBleDeviceConnectionConfig::class to BLEDeviceConnectionApiImpl(
                    context = context,
                    centralManager = scannerModule.centralManager,
                    serialApiFactory = createSerialApiFactory()
                ),
                FMockDeviceConnectionConfig::class to MockDeviceConnectionApiImpl()
            )
        )
    )
}
