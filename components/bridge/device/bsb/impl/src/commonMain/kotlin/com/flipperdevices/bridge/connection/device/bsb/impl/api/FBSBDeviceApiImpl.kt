package com.flipperdevices.bridge.connection.device.bsb.impl.api

import com.flipperdevices.bridge.connection.device.bsb.api.FBSBDeviceApi
import com.flipperdevices.bridge.connection.device.bsb.impl.utils.FZeroFeatureClassToEnumMapper
import com.flipperdevices.bridge.connection.feature.common.api.FDeviceFeature
import com.flipperdevices.bridge.connection.feature.common.api.FDeviceFeatureApi
import com.flipperdevices.bridge.connection.feature.common.api.FOnDeviceReadyFeatureApi
import com.flipperdevices.bridge.connection.feature.common.api.FUnsafeDeviceFeatureApi
import com.flipperdevices.bridge.connection.transport.common.api.FConnectedDeviceApi
import com.flipperdevices.busylib.core.buildkonfig.BuildKonfigBusyBle
import com.flipperdevices.core.busylib.log.LogTagProvider
import com.flipperdevices.core.busylib.log.error
import com.flipperdevices.core.busylib.log.info


import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.reflect.KClass

class FBSBDeviceApiImpl(
    private val scope: CoroutineScope,
    private val connectedDevice: FConnectedDeviceApi,
    onReadyFeaturesApiFactories: Set<FOnDeviceReadyFeatureApi.Factory>,
    private val factories: Map<FDeviceFeature, FDeviceFeatureApi.Factory>
) : FBSBDeviceApi, FUnsafeDeviceFeatureApi, LogTagProvider {
    override val TAG = "FZeroDeviceApi"

    // todo Hi, @Programistich you've had fix for that
    private val features = mutableMapOf<FDeviceFeature, Deferred<FDeviceFeatureApi?>>()
    private val mutex = Mutex()

    init {
        if (BuildKonfigBusyBle.CRASH_APP_ON_FAILED_CHECKS) {
            FDeviceFeature.entries.forEach { key ->
                checkNotNull(factories[key]) { "Not found factory for $key" }
            }
        }

        scope.launch {
            callAllOnReadyDeviceFeatures(onReadyFeaturesApiFactories)
        }
    }

    override suspend fun <T : FDeviceFeatureApi> get(clazz: KClass<T>): Deferred<T?>? =
        mutex.withLock {
            return@withLock getUnsafe(clazz)
        }

    override suspend fun <T : FDeviceFeatureApi> getUnsafe(clazz: KClass<T>): Deferred<T?>? {
        val deviceFeature = FZeroFeatureClassToEnumMapper.get(clazz) ?: return null
        val deferredFeatureApi = getFeatureApi(deviceFeature)
        return scope.async {
            val featureApi = deferredFeatureApi?.await()
            if (!clazz.isInstance(featureApi)) {
                null
            } else {
                featureApi as? T
            }
        }
    }

    private suspend fun getFeatureApi(feature: FDeviceFeature): Deferred<FDeviceFeatureApi?>? {
        var featureApiFlow: Deferred<FDeviceFeatureApi?>? = features[feature]
        if (featureApiFlow != null) {
            return featureApiFlow
        }
        val factory = factories[feature]
        if (factory == null) {
            error { "Fail to find factory for feature $feature" }
            return null
        }
        info { "$feature feature start creation..." }
        featureApiFlow = scope.async {
            val featureApi = factory(
                unsafeFeatureDeviceApi = this@FBSBDeviceApiImpl,
                scope = scope,
                connectedDevice = connectedDevice
            )
            if (featureApi == null) {
                error { "Fail to create $feature!" }
            } else {
                info { "$feature feature creation successful!" }
            }
            featureApi
        }

        features[feature] = featureApiFlow
        return featureApiFlow
    }

    private suspend fun callAllOnReadyDeviceFeatures(
        factories: Set<FOnDeviceReadyFeatureApi.Factory>
    ) = mutex.withLock {
        for (factory in factories) {
            try {
                val featureApi = factory(
                    unsafeFeatureDeviceApi = this,
                    scope = scope,
                    connectedDevice = connectedDevice
                )
                featureApi?.onReady()
            } catch (e: Throwable) {
                error(e) { "Failed init on ready device factory $factory" }
            }
        }
    }

    interface Factory : FBSBDeviceApi.Factory {
        override fun invoke(
            scope: CoroutineScope,
            connectedDevice: FConnectedDeviceApi,
        ): FBSBDeviceApiImpl
    }
}
