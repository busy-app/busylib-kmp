package com.flipperdevices.bridge.connection

import android.app.Application
import com.flipperdevices.bridge.connection.di.AndroidAppComponent
import com.flipperdevices.core.di.ComponentHolder
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

        ComponentHolder.components += androidAppComponent

        Timber.plant(Timber.DebugTree())

        println("rootComponentFactory -> ${ComponentHolder.components}")
//        ComponentHolder.component<AppComponent>().rootComponentFactory
        println("connectionService -> ${ComponentHolder.components}")
//        ComponentHolder.component<AppComponent>().connectionService.onApplicationInit()
//        ComponentHolder.component<AppComponent>().connectionService.onApplicationInit()
    }
}
