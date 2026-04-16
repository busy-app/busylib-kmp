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
    private val scope by lazy {
        CoroutineScope(SupervisorJob() + FlipperDispatchers.default)
    }
    private val hostApi by lazy {
        BUSYLibHostApiStub(
            host = "cloud.dev.busy.app",
        )
    }
    private val settings by lazy {
        SharedPreferencesSettings(
            baseContext.getSharedPreferences("settings", MODE_PRIVATE)
        )
    }
    val principalApi by lazy {
        UserPrincipalApiSampleImpl(scope, hostApi, settings)
    }
    val busyLib: BUSYLibAndroid by lazy {
        BUSYLibAndroid.build(
            scope = scope,
            principalApi = principalApi,
            settings = settings,
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
