//package com.flipperdevices.busylib
//
//import com.flipperdevices.bridge.connection.config.api.FDevicePersistedStorage
//import com.flipperdevices.bridge.connection.service.api.FConnectionService
//import com.flipperdevices.bsb.auth.principal.api.BsbUserPrincipalApi
//import com.flipperdevices.bsb.cloud.api.BSBBarsApi
//import com.flipperdevices.busylib.di.BUSYLibGraphiOS
//import dev.zacsweers.metro.Inject
//import dev.zacsweers.metro.createGraphFactory
//import kotlinx.coroutines.CoroutineScope
//
//@Inject
//class BUSYLibiOS(
//    override val connectionService: FConnectionService
//) : BUSYLib {
//    companion object {
//        fun build(
//            scope: CoroutineScope,
//            principalApi: BsbUserPrincipalApi,
//            bsbBarsApi: BSBBarsApi,
//            persistedStorage: FDevicePersistedStorage,
//        ): BUSYLibiOS {
//            val graph = createGraphFactory<BUSYLibGraphiOS.Factory>()
//                .create(
//                    scope,
//                    principalApi,
//                    bsbBarsApi,
//                    persistedStorage
//                )
//            return graph.busyLib
//        }
//    }
//}