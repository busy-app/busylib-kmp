package net.flipper.bridge.impl.di

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import me.tatarka.inject.annotations.Provides
import net.flipper.busylib.core.di.BusyLibGraph
import no.nordicsemi.kotlin.ble.client.android.CentralManager
import no.nordicsemi.kotlin.ble.client.android.native
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@ContributesTo(BusyLibGraph::class)
interface NordicBleModule {
    @Provides
    @SingleIn(BusyLibGraph::class)
    fun provideCentralManager(context: Context, scope: CoroutineScope): CentralManager {
        return CentralManager.native(context, scope)
    }

    @Provides
    fun provideBluetoothAdapter(
        bluetoothManager: BluetoothManager?
    ): BluetoothAdapter {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
                @Suppress("DEPRECATION")
                bluetoothManager?.adapter ?: BluetoothAdapter.getDefaultAdapter()

            else ->
                @Suppress("DEPRECATION")
                BluetoothAdapter.getDefaultAdapter()
        }
    }

    @Provides
    fun provideBluetoothManager(context: Context): BluetoothManager? {
        return ContextCompat.getSystemService(context, BluetoothManager::class.java)
    }
}
