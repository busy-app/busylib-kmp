package com.flipperdevices.busylib.di

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Build
import androidx.core.content.ContextCompat
import com.flipperdevices.bridge.api.scanner.FlipperScanner
import com.flipperdevices.bridge.impl.scanner.FlipperScannerImpl
import kotlinx.coroutines.CoroutineScope
import no.nordicsemi.kotlin.ble.client.android.CentralManager
import no.nordicsemi.kotlin.ble.client.android.native

actual class ScannerModule(
    scope: CoroutineScope,
    private val context: Context
) {
    fun getBluetoothManager(): BluetoothManager? {
        return ContextCompat.getSystemService(context, BluetoothManager::class.java)
    }

    val bluetoothAdapter = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            @Suppress("DEPRECATION")
            getBluetoothManager()?.adapter ?: BluetoothAdapter.getDefaultAdapter()

        else ->
            @Suppress("DEPRECATION")
            BluetoothAdapter.getDefaultAdapter()
    }


    val centralManager: CentralManager = CentralManager.Factory.native(context, scope)

    val flipperScanner: FlipperScanner = FlipperScannerImpl(
        centralManager = centralManager,
        bluetoothAdapter = bluetoothAdapter,
        context = context
    )
}
