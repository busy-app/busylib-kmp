package com.flipperdevices.bridge.connection

import android.app.Application
import com.flipperdevices.bridge.connection.utils.cloud.BSBBarsApiNoop
import com.flipperdevices.bridge.connection.utils.config.impl.FDevicePersistedStorageImpl
import com.flipperdevices.bridge.connection.utils.principal.impl.UserPrincipalApiNoop
import com.flipperdevices.busylib.BUSYLibAndroid
import com.russhwolf.settings.SharedPreferencesSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class ConnectionTestApplication : Application() {
    val busyLib: BUSYLibAndroid by lazy {
        BUSYLibAndroid.build(
            CoroutineScope(SupervisorJob()),
            principalApi = UserPrincipalApiNoop(),
            bsbBarsApi = BSBBarsApiNoop(),
            persistedStorage = FDevicePersistedStorageImpl(
                SharedPreferencesSettings(
                    baseContext.getSharedPreferences(
                        "settings",
                        MODE_PRIVATE
                    )
                )
            ),
            context = this
        )
    }

    override fun onCreate() {
        super.onCreate()

        busyLib.connectionService.onApplicationInit()
    }
}
