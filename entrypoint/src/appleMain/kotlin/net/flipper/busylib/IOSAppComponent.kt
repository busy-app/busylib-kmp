package net.flipper.busylib

import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.ObservableSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import platform.Foundation.NSUserDefaults

fun getBUSYLibIOSScope(): CoroutineScope {
    return CoroutineScope(SupervisorJob())
}

fun getObservationSettings(delegate: NSUserDefaults): ObservableSettings {
    return NSUserDefaultsSettings(delegate = delegate)
}
