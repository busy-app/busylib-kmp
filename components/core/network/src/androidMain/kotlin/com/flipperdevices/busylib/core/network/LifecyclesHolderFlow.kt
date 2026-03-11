package com.flipperdevices.busylib.core.network

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
    initList: List<LifecycleWithState>
) {
    data class LifecycleWithState(
        val lifecycle: Lifecycle,
        val shouldBeState: Lifecycle.State = Lifecycle.State.STARTED
    )

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

    fun addLifecycle(lifecycle: Lifecycle, shouldBeState: Lifecycle.State = Lifecycle.State.STARTED) {
        lifecyclesStateFlow.update {
            if (it.none { entry -> entry.lifecycle == lifecycle }) {
                it.plus(LifecycleWithState(lifecycle, shouldBeState))
            } else {
                it
            }
        }
    }

    private fun getLifecyclesFlow(): Flow<List<Flow<Boolean>>> {
        return lifecyclesStateFlow.map { lifecycles ->
            lifecycles.map { entry ->
                entry.lifecycle.currentStateFlow
                    .onEach { state ->
                        if (state == Lifecycle.State.DESTROYED) {
                            lifecyclesStateFlow.update { it.filter { e -> e.lifecycle != entry.lifecycle } }
                        }
                    }.map { state ->
                        state.isAtLeast(entry.shouldBeState)
                    }
            }
        }
    }
}
