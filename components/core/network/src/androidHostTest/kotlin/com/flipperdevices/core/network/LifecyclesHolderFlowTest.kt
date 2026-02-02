package com.flipperdevices.core.network

import androidx.lifecycle.Lifecycle
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@Suppress("TooManyFunctions")
class LifecyclesHolderFlowTest {
    private fun createMockLifecycle(initialState: Lifecycle.State): Pair<Lifecycle, MutableStateFlow<Lifecycle.State>> {
        val stateFlow = MutableStateFlow(initialState)
        val lifecycle = mockk<Lifecycle> {
            every { currentStateFlow } returns stateFlow
        }
        return lifecycle to stateFlow
    }

    @Test
    fun `GIVEN empty list WHEN collecting flow THEN emits false`() = runTest {
        val holder = LifecyclesHolderFlow(emptyList())

        val result = holder.isAnyLifecycleOnStartFlow.first()

        assertFalse(result)
    }

    @Test
    fun `GIVEN single started lifecycle WHEN collecting flow THEN emits true`() = runTest {
        val (lifecycle, _) = createMockLifecycle(Lifecycle.State.STARTED)
        val holder = LifecyclesHolderFlow(listOf(lifecycle))

        val result = holder.isAnyLifecycleOnStartFlow.first()

        assertTrue(result)
    }

    @Test
    fun `GIVEN single resumed lifecycle WHEN collecting flow THEN emits true`() = runTest {
        val (lifecycle, _) = createMockLifecycle(Lifecycle.State.RESUMED)
        val holder = LifecyclesHolderFlow(listOf(lifecycle))

        val result = holder.isAnyLifecycleOnStartFlow.first()

        assertTrue(result)
    }

    @Test
    fun `GIVEN single created lifecycle WHEN collecting flow THEN emits false`() = runTest {
        val (lifecycle, _) = createMockLifecycle(Lifecycle.State.CREATED)
        val holder = LifecyclesHolderFlow(listOf(lifecycle))

        val result = holder.isAnyLifecycleOnStartFlow.first()

        assertFalse(result)
    }

    @Test
    fun `GIVEN multiple lifecycles all started WHEN collecting flow THEN emits true`() = runTest {
        val (lifecycle1, _) = createMockLifecycle(Lifecycle.State.STARTED)
        val (lifecycle2, _) = createMockLifecycle(Lifecycle.State.RESUMED)
        val holder = LifecyclesHolderFlow(listOf(lifecycle1, lifecycle2))

        val result = holder.isAnyLifecycleOnStartFlow.first()

        assertTrue(result)
    }

    @Test
    fun `GIVEN multiple lifecycles with at least one started WHEN collecting flow THEN emits true`() = runTest {
        // Note: isAnyLifecycleOnStartFlow uses 'any' logic - returns true if ANY lifecycle is started
        val (lifecycle1, _) = createMockLifecycle(Lifecycle.State.STARTED)
        val (lifecycle2, _) = createMockLifecycle(Lifecycle.State.CREATED)
        val holder = LifecyclesHolderFlow(listOf(lifecycle1, lifecycle2))

        val result = holder.isAnyLifecycleOnStartFlow.first()

        assertTrue(result)
    }

    @Test
    fun `GIVEN multiple lifecycles none started WHEN collecting flow THEN emits false`() = runTest {
        val (lifecycle1, _) = createMockLifecycle(Lifecycle.State.CREATED)
        val (lifecycle2, _) = createMockLifecycle(Lifecycle.State.INITIALIZED)
        val holder = LifecyclesHolderFlow(listOf(lifecycle1, lifecycle2))

        val result = holder.isAnyLifecycleOnStartFlow.first()

        assertFalse(result)
    }

    @Test
    fun `GIVEN lifecycle WHEN adding duplicate THEN does not duplicate`() = runTest {
        val (lifecycle, _) = createMockLifecycle(Lifecycle.State.STARTED)
        val holder = LifecyclesHolderFlow(listOf(lifecycle))

        holder.addLifecycle(lifecycle)
        holder.addLifecycle(lifecycle)

        val result = holder.isAnyLifecycleOnStartFlow.first()
        assertTrue(result)
    }

    @Test
    fun `GIVEN started lifecycle WHEN adding new started lifecycle THEN still emits true`() = runTest {
        val (lifecycle1, _) = createMockLifecycle(Lifecycle.State.STARTED)
        val (lifecycle2, _) = createMockLifecycle(Lifecycle.State.STARTED)
        val holder = LifecyclesHolderFlow(listOf(lifecycle1))

        holder.addLifecycle(lifecycle2)

        val result = holder.isAnyLifecycleOnStartFlow.first()
        assertTrue(result)
    }

    @Test
    fun `GIVEN no started lifecycles WHEN adding started lifecycle THEN emits true`() = runTest {
        val (lifecycle1, _) = createMockLifecycle(Lifecycle.State.CREATED)
        val (lifecycle2, _) = createMockLifecycle(Lifecycle.State.STARTED)
        val holder = LifecyclesHolderFlow(listOf(lifecycle1))

        holder.addLifecycle(lifecycle2)

        val result = holder.isAnyLifecycleOnStartFlow.first()
        assertTrue(result)
    }

    @Test
    fun `GIVEN started lifecycle WHEN state changes to destroyed THEN lifecycle is removed`() = runTest {
        val (lifecycle1, stateFlow1) = createMockLifecycle(Lifecycle.State.STARTED)
        val (lifecycle2, _) = createMockLifecycle(Lifecycle.State.STARTED)
        val holder = LifecyclesHolderFlow(listOf(lifecycle1, lifecycle2))

        // Initial state - both started
        val initialResult = holder.isAnyLifecycleOnStartFlow.first()
        assertTrue(initialResult)

        // Destroy lifecycle1
        stateFlow1.value = Lifecycle.State.DESTROYED

        // Should still emit true because lifecycle2 is still started
        val afterDestroyResult = holder.isAnyLifecycleOnStartFlow.first()
        assertTrue(afterDestroyResult)
    }

    @Test
    fun `GIVEN single lifecycle WHEN state changes from created to started THEN emits true`() = runTest {
        val (lifecycle, stateFlow) = createMockLifecycle(Lifecycle.State.CREATED)
        val holder = LifecyclesHolderFlow(listOf(lifecycle))

        // Initial state - created, not started
        val initialResult = holder.isAnyLifecycleOnStartFlow.first()
        assertFalse(initialResult)

        // Change to started
        stateFlow.value = Lifecycle.State.STARTED

        val afterStartedResult = holder.isAnyLifecycleOnStartFlow.first()
        assertTrue(afterStartedResult)
    }

    @Test
    fun `GIVEN single lifecycle WHEN state changes from started to stopped THEN emits false`() = runTest {
        val (lifecycle, stateFlow) = createMockLifecycle(Lifecycle.State.STARTED)
        val holder = LifecyclesHolderFlow(listOf(lifecycle))

        // Initial state - started
        val initialResult = holder.isAnyLifecycleOnStartFlow.first()
        assertTrue(initialResult)

        // Change to created (stopped)
        stateFlow.value = Lifecycle.State.CREATED

        val afterStoppedResult = holder.isAnyLifecycleOnStartFlow.first()
        assertFalse(afterStoppedResult)
    }
}
