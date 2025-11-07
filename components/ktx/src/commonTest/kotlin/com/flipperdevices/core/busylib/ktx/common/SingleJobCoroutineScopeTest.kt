package com.flipperdevices.core.busylib.ktx.common

import com.flipperdevices.core.busylib.ktx.common.SingleJobMode
import com.flipperdevices.core.busylib.ktx.common.asSingleJobScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SingleJobCoroutineScopeTest {
    @Test
    fun GIVEN_skip_if_running_WHEN_run_twice_THEN_one_result() = runTest {
        val singleJobScope = asSingleJobScope()
        val incrementFlow = MutableStateFlow(0)
        val jobs = listOf(
            singleJobScope.launch(SingleJobMode.SKIP_IF_RUNNING) {
                delay(200L)
                incrementFlow.update { value -> value + 1 }
            },
            singleJobScope.launch(SingleJobMode.SKIP_IF_RUNNING) {
                delay(200L)
                incrementFlow.update { value -> value + 1 }
            }
        )
        advanceTimeBy(200L * 2 + 1L)
        jobs.joinAll()
        assertEquals(1, incrementFlow.first())
    }

    @Test
    fun GIVEN_await_if_running_WHEN_run_multple_THEN_multiple_result() = runTest {
        val singleJobScope = asSingleJobScope()
        val incrementFlow = MutableStateFlow(0)
        val jobs = List(5) {
            singleJobScope.launch(SingleJobMode.AWAIT_PREVIOUS) {
                println("Lauynched:L $it")
                delay(200L)
                incrementFlow.update { value ->
                    value + 1
                }
            }
        }
        advanceTimeBy(200L * 5 + 1L)
        jobs.joinAll()
        assertEquals(5, incrementFlow.first())
    }

    @Test
    fun GIVEN_cancel_if_running_WHEN_run_multiple_THEN_one_result() = runTest {
        val singleJobScope = asSingleJobScope()
        val incrementFlow = MutableStateFlow(0)
        val jobs = List(5) {
            singleJobScope.launch(SingleJobMode.CANCEL_PREVIOUS) {
                delay(200L)
                incrementFlow.update { value ->
                    value + 1
                }
            }
        }
        advanceTimeBy(200L * 5 + 1L)
        jobs.joinAll()
        assertEquals(1, incrementFlow.first())
    }

    @Test
    fun GIVEN_suspend_then_cancel_WHEN_cancel_last_THEN_cancelled() = runTest {
        val singleJobScope = asSingleJobScope()
        val incrementFlow = MutableStateFlow(0)
        val jobs = listOf(
            singleJobScope.launch(SingleJobMode.SKIP_IF_RUNNING) {
                delay(200L)
                incrementFlow.update { value ->
                    value + 10
                }
            },
            singleJobScope.launch(SingleJobMode.AWAIT_PREVIOUS) {
                delay(200L)
                incrementFlow.update { value ->
                    value + 100
                }
            },
            singleJobScope.launch(SingleJobMode.CANCEL_PREVIOUS) {
                delay(200L)
                incrementFlow.update { value ->
                    value - 100
                }
            }
        )
        advanceTimeBy(200L * 5 + 1L)
        jobs.joinAll()
        assertEquals(-100, incrementFlow.first())
    }
}
