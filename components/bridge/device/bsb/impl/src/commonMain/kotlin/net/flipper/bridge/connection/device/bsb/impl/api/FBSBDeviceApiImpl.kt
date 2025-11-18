package net.flipper.bridge.connection.device.bsb.impl.api

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.device.bsb.api.FBSBDeviceApi
import net.flipper.bridge.connection.device.bsb.impl.utils.FZeroFeatureClassToEnumMapper
import net.flipper.bridge.connection.feature.common.api.FDeviceFeature
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.common.api.FOnDeviceReadyFeatureApi
import net.flipper.bridge.connection.feature.common.api.FUnsafeDeviceFeatureApi
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.busylib.kmp.components.core.buildkonfig.BuildKonfig
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.error
import net.flipper.core.busylib.log.info
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import kotlin.reflect.KClass

@Inject
class FBSBDeviceApiImpl(
    @Assisted private val scope: CoroutineScope,
    @Assisted private val connectedDevice: FConnectedDeviceApi,
    onReadyFeaturesApiFactories: Set<FOnDeviceReadyFeatureApi.Factory>,
    private val factories: Map<FDeviceFeature, FDeviceFeatureApi.Factory>
) : FBSBDeviceApi, FUnsafeDeviceFeatureApi, LogTagProvider {
    override val TAG = "FZeroDeviceApi"

    private val features = mutableMapOf<FDeviceFeature, Deferred<FDeviceFeatureApi?>>()
    private val mutex = Mutex()

    init {
        if (BuildKonfig.CRASH_APP_ON_FAILED_CHECKS) {
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
    ) = factories.map { factory ->
        scope.async {
            try {
                val featureApi = when (factory) {
                    is FDeviceFeatureApi.Factory -> {
                        this@FBSBDeviceApiImpl.factories
                            .toList()
                            .firstOrNull { (_, onDemandFeatureFactory) -> onDemandFeatureFactory == factory }
                            ?.first
                            ?.let { fDeviceFeature -> getFeatureApi(fDeviceFeature) }
                            ?.await()
                    }

                    else -> {
                        factory(
                            unsafeFeatureDeviceApi = this@FBSBDeviceApiImpl,
                            scope = scope,
                            connectedDevice = connectedDevice
                        )
                    }
                }
                (featureApi as? FOnDeviceReadyFeatureApi)?.onReady()
            } catch (e: Throwable) {
                error(e) { "Failed init on ready device factory $factory" }
            }
        }
    }.awaitAll()

    @Inject
    @ContributesBinding(BusyLibGraph::class, FBSBDeviceApi.Factory::class)
    class Factory(
        private val factory: (CoroutineScope, FConnectedDeviceApi) -> FBSBDeviceApiImpl
    ) : FBSBDeviceApi.Factory {
        override fun invoke(
            scope: CoroutineScope,
            connectedDevice: FConnectedDeviceApi,
        ): FBSBDeviceApiImpl = factory(scope, connectedDevice)
    }
}
