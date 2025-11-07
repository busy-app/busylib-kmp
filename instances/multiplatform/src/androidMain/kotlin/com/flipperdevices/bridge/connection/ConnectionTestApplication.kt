package com.flipperdevices.bridge.connection

import android.app.Application
import com.flipperdevices.bridge.connection.di.AndroidAppComponent
import com.flipperdevices.busylib.core.di.BusyLibComponentHolder
import com.russhwolf.settings.SharedPreferencesSettings
import dev.zacsweers.metro.createGraphFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import timber.log.Timber

class ConnectionTestApplication : Application() {
    private val androidAppComponent by lazy {
        createGraphFactory<AndroidAppComponent.Factory>().create(
            observableSettings = SharedPreferencesSettings(
                baseContext.getSharedPreferences(
                    "settings",
                    MODE_PRIVATE
                )
            ),
            scope = CoroutineScope(SupervisorJob()),
            context = this,
        )
    }

    override fun onCreate() {
        super.onCreate()

        BusyLibComponentHolder.components += androidAppComponent

        Timber.plant(Timber.DebugTree())

        BusyLibComponentHolder.component<AndroidAppComponent>().connectionService.onApplicationInit()
    }
}
