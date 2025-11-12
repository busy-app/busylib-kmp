package net.flipper.busylib.di

import com.russhwolf.settings.NSUserDefaultsSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import net.flipper.bridge.connection.config.api.FDevicePersistedStorage
import net.flipper.bridge.connection.config.impl.FDevicePersistedStorageImpl
import net.flipper.bsb.auth.principal.api.BsbUserPrincipalApi
import platform.Foundation.NSUserDefaults

fun getBUSYLibIOSScope(): CoroutineScope {
    return CoroutineScope(SupervisorJob())
}

fun getPersistedStorage(delegate: NSUserDefaults): FDevicePersistedStorage {
    return FDevicePersistedStorageImpl(NSUserDefaultsSettings(delegate))
}

fun getBsbUserPrincipalApi(): BsbUserPrincipalApi {
    return BUSYLibPrincipalApiImpl()
}
