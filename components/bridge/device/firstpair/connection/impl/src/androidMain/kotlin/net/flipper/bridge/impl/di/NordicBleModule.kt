package net.flipper.bridge.impl.di

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Build
import androidx.core.content.ContextCompat
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import net.flipper.busylib.core.di.BusyLibGraph
import no.nordicsemi.kotlin.ble.client.android.CentralManager
import no.nordicsemi.kotlin.ble.client.android.native
import no.nordicsemi.kotlin.ble.environment.android.NativeAndroidEnvironment

@ContributesTo(BusyLibGraph::class)
@BindingContainer
object NordicBleModule {
    @Provides
    @SingleIn(BusyLibGraph::class)
    fun provideCentralManager(context: Context, scope: CoroutineScope): CentralManager {
        val environment = NativeAndroidEnvironment.getInstance(
            context = context,
            isNeverForLocationFlagSet = true
        )
        return CentralManager.native(environment, scope)
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
