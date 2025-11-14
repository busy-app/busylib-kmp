package net.flipper.busylib.di

import com.russhwolf.settings.NSUserDefaultsSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import net.flipper.bridge.connection.config.api.FDevicePersistedStorage
import net.flipper.bridge.connection.config.impl.FDevicePersistedStorageImpl
import net.flipper.bridge.connection.feature.battery.api.FDeviceBatteryInfoFeatureApi
import net.flipper.bridge.connection.feature.provider.api.FFeatureProvider
import net.flipper.bridge.connection.feature.provider.api.FFeatureStatus
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import net.flipper.bsb.auth.principal.api.BUSYLibPrincipalApi
import platform.Foundation.NSUserDefaults

fun getBUSYLibIOSScope(): CoroutineScope {
    return CoroutineScope(SupervisorJob())
}

fun getPersistedStorage(delegate: NSUserDefaults): FDevicePersistedStorage {
    return FDevicePersistedStorageImpl(NSUserDefaultsSettings(delegate))
}

fun getBsbUserPrincipalApi(): BUSYLibPrincipalApi {
    return BUSYLibPrincipalApiImpl()
}

fun FFeatureProvider.getBatteryInfoFeature(): Flow<FFeatureStatus<FDeviceBatteryInfoFeatureApi>> {
    return get(FDeviceBatteryInfoFeatureApi::class)
}

fun FFeatureProvider.getRPC(): Flow<FFeatureStatus<FRpcFeatureApi>> {
    return get(FRpcFeatureApi::class)
}