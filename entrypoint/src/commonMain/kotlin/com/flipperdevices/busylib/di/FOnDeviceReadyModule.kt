package com.flipperdevices.busylib.di

import com.flipperdevices.bridge.connection.feature.link.check.ondemand.api.FLinkedInfoOnDemandFeatureApi
import com.flipperdevices.bridge.connection.feature.link.check.onready.api.FLinkInfoOnReadyFeatureApiImpl
import com.flipperdevices.bridge.connection.feature.link.check.onready.api.FLinkInfoOnReadyFeatureFactoryImpl
import com.flipperdevices.bridge.connection.feature.sync.impl.FTimerSyncFactoryImpl
import com.flipperdevices.bridge.connection.feature.sync.impl.FTimerSyncFeatureApiImpl
import kotlinx.coroutines.CoroutineScope

class FOnDeviceReadyModule {
    private val fTimerSyncFactoryImpl: FTimerSyncFactoryImpl = FTimerSyncFactoryImpl(
        timerSyncFeatureFactory = object : FTimerSyncFeatureApiImpl.InternalFactory {
            override fun invoke(
                scope: CoroutineScope
            ): FTimerSyncFeatureApiImpl {
                return FTimerSyncFeatureApiImpl(scope)
            }
        }
    )

    private val fLinkInfoOnReadyFeatureFactoryImpl: FLinkInfoOnReadyFeatureFactoryImpl = FLinkInfoOnReadyFeatureFactoryImpl(
        fLinkInfoOnReadyFeatureApiImpl = object : FLinkInfoOnReadyFeatureApiImpl.InternalFactory {
            override fun invoke(
                fLinkedInfoOnDemandFeatureApi: FLinkedInfoOnDemandFeatureApi
            ): FLinkInfoOnReadyFeatureApiImpl {
                return FLinkInfoOnReadyFeatureApiImpl(fLinkedInfoOnDemandFeatureApi)
            }
        }
    )

    val onReadyFeaturesApiFactories = setOf(
        fTimerSyncFactoryImpl,
        fLinkInfoOnReadyFeatureFactoryImpl
    )
}
