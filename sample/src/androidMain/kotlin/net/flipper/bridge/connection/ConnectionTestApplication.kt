package net.flipper.bridge.connection

import android.app.Application
import com.russhwolf.settings.SharedPreferencesSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import net.flipper.bridge.connection.utils.cloud.BSBBarsApiNoop
import net.flipper.bridge.connection.utils.config.impl.FDevicePersistedStorageImpl
import net.flipper.bridge.connection.utils.principal.impl.UserPrincipalApiNoop
import net.flipper.busylib.BUSYLibAndroid

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
    val busyLib: BUSYLibAndroid by lazy {
        BUSYLibAndroid.build(
            CoroutineScope(SupervisorJob()),
            principalApi = UserPrincipalApiNoop(),
            bsbBarsApi = BSBBarsApiNoop(),
            persistedStorage = persistedStorage,
            context = this
        )
    }

    override fun onCreate() {
        super.onCreate()

        busyLib.connectionService.onApplicationInit()
    }
}
