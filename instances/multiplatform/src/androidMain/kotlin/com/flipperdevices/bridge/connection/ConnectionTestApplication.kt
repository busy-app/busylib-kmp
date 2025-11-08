package com.flipperdevices.bridge.connection

import android.app.Application
import com.flipperdevices.bridge.connection.utils.cloud.BSBBarsApiNoop
import com.flipperdevices.bridge.connection.utils.config.impl.FDevicePersistedStorageImpl
import com.flipperdevices.bridge.connection.utils.principal.impl.UserPrincipalApiNoop
import com.flipperdevices.busylib.di.AndroidFDeviceHolderFactoryModule
import com.flipperdevices.busylib.di.PlatformModule
import com.flipperdevices.busylib.di.RootModule
import com.flipperdevices.busylib.di.ScannerModule
import com.russhwolf.settings.SharedPreferencesSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class ConnectionTestApplication : Application() {
    val persistedStorage by lazy {
        FDevicePersistedStorageImpl(
            SharedPreferencesSettings(
                baseContext.getSharedPreferences(
                    "settings",
                    MODE_PRIVATE
                )
            )
        )
    }
    val rootModule: RootModule by lazy {
        val scope = CoroutineScope(SupervisorJob())
        val scannerModule = ScannerModule(
            scope = scope,
            context = applicationContext,
        )
        RootModule(
            principalApi = UserPrincipalApiNoop(),
            bsbBarsApi = BSBBarsApiNoop(),
            persistedStorage = persistedStorage,
            scannerModule = scannerModule,
            fDeviceHolderFactoryModule = AndroidFDeviceHolderFactoryModule(
                scannerModule = scannerModule,
                context = applicationContext
            ),
        )
    }

    override fun onCreate() {
        super.onCreate()

        rootModule.deviceModule.connectionService.onApplicationInit()
    }
}
