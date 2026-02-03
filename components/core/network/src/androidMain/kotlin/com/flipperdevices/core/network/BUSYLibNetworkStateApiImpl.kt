package com.flipperdevices.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.info

class BUSYLibNetworkStateApiImpl(
    context: Context,
    scope: CoroutineScope
) : BUSYLibAndroidNetworkStateApi, ConnectivityManager.NetworkCallback(), LogTagProvider {
    override val TAG = "NetworkStateApi"
    private val lifecycleHolder = LifecyclesHolderFlow(
        listOf(ProcessLifecycleOwner.get().lifecycle)
    )
    private val isNetworkAvailableFlowInternal = MutableStateFlow(false)

    override val isNetworkAvailableFlow = combine(
        isNetworkAvailableFlowInternal,
        lifecycleHolder.isAnyLifecycleOnStartFlow
    ) { isNetworkAvailable, isAllLifecyclesOnStart ->
        info { "Is network available: $isNetworkAvailable, is all lifecycle on start: $isAllLifecyclesOnStart" }
        isNetworkAvailable && isAllLifecyclesOnStart
    }.stateIn(scope, SharingStarted.Eagerly, false)

    init {
        val connectivityManager =
            ContextCompat.getSystemService(context, ConnectivityManager::class.java)
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        info { "Init with network request $networkRequest and manager $connectivityManager" }
        connectivityManager?.requestNetwork(networkRequest, this)
    }

    override fun onAvailable(network: Network) {
        info { "Network available: $network" }
        super.onAvailable(network)
        isNetworkAvailableFlowInternal.value = true
    }

    override fun onLost(network: Network) {
        info { "Network lost: $network" }
        super.onLost(network)
        isNetworkAvailableFlowInternal.value = false
    }

    override fun addLifecycle(lifecycle: Lifecycle) {
        info { "Add lifecycle $lifecycle" }
        lifecycleHolder.addLifecycle(lifecycle)
    }
}
