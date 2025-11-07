package com.flipperdevices.bridge.connection.di

import com.flipperdevices.bridge.connection.utils.cloud.BSBBarsApiNoop
import com.flipperdevices.bridge.connection.utils.config.impl.FDevicePersistedStorageImpl
import com.flipperdevices.bridge.connection.utils.principal.impl.UserPrincipalApiNoop
import com.flipperdevices.busylib.BUSYLibiOS
import com.russhwolf.settings.NSUserDefaultsSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import platform.Foundation.NSUserDefaults

val busyLib: BUSYLibiOS by lazy {
    BUSYLibiOS.build(
        CoroutineScope(SupervisorJob()),
        principalApi = UserPrincipalApiNoop(),
        bsbBarsApi = BSBBarsApiNoop(),
        persistedStorage = FDevicePersistedStorageImpl(
            NSUserDefaultsSettings(
                NSUserDefaults.standardUserDefaults
            )
        )
    )
}