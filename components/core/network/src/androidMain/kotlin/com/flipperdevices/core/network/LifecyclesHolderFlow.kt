package com.flipperdevices.core.network

import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

class LifecyclesHolderFlow(
    initList: List<Lifecycle>
) {
    private val lifecyclesStateFlow = MutableStateFlow(initList)
    val isAnyLifecycleOnStartFlow = getLifecyclesFlow()
        .flatMapLatest { flows ->
            if (flows.isEmpty()) {
                flowOf(false)
            } else {
                combine(flows) { list ->
                    list.any { it }
                }
            }
        }

    fun addLifecycle(lifecycle: Lifecycle) {
        lifecyclesStateFlow.update {
            if (!it.contains(lifecycle)) {
                it.plus(lifecycle)
            } else {
                it
            }
        }
    }

    private fun getLifecyclesFlow(): Flow<List<Flow<Boolean>>> {
        return lifecyclesStateFlow.map { lifecycles ->
            lifecycles.map { lifecycle ->
                lifecycle.currentStateFlow
                    .onEach { state ->
                        if (state == Lifecycle.State.DESTROYED) {
                            lifecyclesStateFlow.update { it.minus(lifecycle) }
                        }
                    }.map { state ->
                        state.isAtLeast(Lifecycle.State.STARTED)
                    }
            }
        }
    }
}
