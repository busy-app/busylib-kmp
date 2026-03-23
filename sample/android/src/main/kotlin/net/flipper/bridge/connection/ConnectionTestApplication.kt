package net.flipper.bridge.connection

import android.app.Application
import com.flipperdevices.busylib.core.network.BUSYLibNetworkStateApiImpl
import com.russhwolf.settings.SharedPreferencesSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import net.flipper.bridge.connection.utils.principal.impl.UserPrincipalApiSampleImpl
import net.flipper.bsb.cloud.api.BUSYLibHostApiStub
import net.flipper.busylib.BUSYLibAndroid
import net.flipper.core.busylib.ktx.common.FlipperDispatchers
import timber.log.Timber

class ConnectionTestApplication : Application() {
    val busyLib: BUSYLibAndroid by lazy {
        val scope = CoroutineScope(SupervisorJob() + FlipperDispatchers.default)
        val hostApi = BUSYLibHostApiStub(
            host = "cloud.dev.busy.app",
        )
        BUSYLibAndroid.build(
            scope = scope,
            principalApi = UserPrincipalApiSampleImpl(scope, hostApi),
            settings = SharedPreferencesSettings(
                baseContext.getSharedPreferences("settings", MODE_PRIVATE)
            ),
            context = this,
            networkStateApi = BUSYLibNetworkStateApiImpl(this, scope),
            hostApi = hostApi
        )
    }

    override fun onCreate() {
        super.onCreate()

        Timber.plant(Timber.DebugTree())

        busyLib.launch()
    }
}
