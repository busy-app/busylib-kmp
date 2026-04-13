package net.flipper.bridge.connection.transport.ble.impl.streaming

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.flipper.bridge.connection.transport.common.api.serial.StatusStreamingEvent
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class FBleStatusStreamingApiImplTest {
    @Test
    fun GIVEN_chunks_within_timeout_WHEN_pause_exceeds_threshold_THEN_single_packet_emitted() = runTest {
        val source = MutableSharedFlow<ByteArray>()
        val packets = mutableListOf<ByteArray>()
        val sut = createSut(source, backgroundScope)

        backgroundScope.launch {
            sut.getPackets().collect { packets.add(it) }
        }
        runCurrent()

        source.emit(byteArrayOf(1, 2))
        advanceTimeBy(100)
        source.emit(byteArrayOf(3, 4))
        advanceTimeBy(249)
        runCurrent()

        assertTrue(packets.isEmpty())

        advanceTimeBy(1)
        runCurrent()

        assertEquals(1, packets.size)
        assertContentEquals(byteArrayOf(1, 2, 3, 4), packets.single())
    }

    @Test
    fun GIVEN_pause_exceeds_timeout_WHEN_new_chunk_arrives_THEN_new_packet_started() = runTest {
        val source = MutableSharedFlow<ByteArray>()
        val packets = mutableListOf<ByteArray>()
        val sut = createSut(source, backgroundScope)

        backgroundScope.launch {
            sut.getPackets().collect { packets.add(it) }
        }
        runCurrent()

        source.emit(byteArrayOf(1))
        advanceTimeBy(250)
        runCurrent()
        source.emit(byteArrayOf(2, 3))
        advanceTimeBy(250)
        runCurrent()

        assertEquals(2, packets.size)
        assertContentEquals(byteArrayOf(1), packets[0])
        assertContentEquals(byteArrayOf(2, 3), packets[1])
    }

    @Test
    fun GIVEN_chunks_arrive_before_timeout_WHEN_timer_restarts_THEN_emit_only_after_last_chunk() = runTest {
        val source = MutableSharedFlow<ByteArray>()
        val packets = mutableListOf<ByteArray>()
        val sut = createSut(source, backgroundScope)

        backgroundScope.launch {
            sut.getPackets().collect { packets.add(it) }
        }
        runCurrent()

        source.emit(byteArrayOf(1))
        advanceTimeBy(200)
        source.emit(byteArrayOf(2))
        advanceTimeBy(200)
        runCurrent()

        assertTrue(packets.isEmpty())

        advanceTimeBy(50)
        runCurrent()

        assertEquals(1, packets.size)
        assertContentEquals(byteArrayOf(1, 2), packets.single())
    }

    @Test
    fun GIVEN_upstream_completes_before_timeout_WHEN_buffer_has_data_THEN_packet_flushed_immediately() = runTest {
        val sut = createSut(
            source = flow {
                emit(byteArrayOf(1))
                emit(byteArrayOf(2, 3))
            },
            scope = backgroundScope
        )

        val packet = sut.getPackets().first()

        assertContentEquals(byteArrayOf(1, 2, 3), packet)
    }

    @Test
    fun GIVEN_upstream_mutates_emitted_chunk_WHEN_packet_emitted_THEN_data_uses_original_bytes() = runTest {
        val sharedChunk = byteArrayOf(1, 2, 3)
        val sut = createSut(
            source = flow {
                emit(sharedChunk)
                sharedChunk[0] = 9
            },
            scope = backgroundScope
        )

        val packet = sut.getPackets().first()

        assertContentEquals(byteArrayOf(1, 2, 3), packet)
    }

    @Test
    fun GIVEN_single_chunk_WHEN_timeout_passes_THEN_same_chunk_emitted_as_packet() = runTest {
        val source = MutableSharedFlow<ByteArray>()
        val packets = mutableListOf<ByteArray>()
        val sut = createSut(source, backgroundScope)

        backgroundScope.launch {
            sut.getPackets().collect { packets.add(it) }
        }
        runCurrent()

        source.emit(byteArrayOf(9, 8, 7))
        advanceTimeBy(250)
        runCurrent()

        assertEquals(1, packets.size)
        assertContentEquals(byteArrayOf(9, 8, 7), packets.single())
    }

    @Test
    fun GIVEN_empty_chunk_WHEN_timeout_passes_THEN_no_packet_emitted() = runTest {
        val source = MutableSharedFlow<ByteArray>()
        val packets = mutableListOf<ByteArray>()
        val sut = createSut(source, backgroundScope)

        backgroundScope.launch {
            sut.getPackets().collect { packets.add(it) }
        }
        runCurrent()

        source.emit(byteArrayOf())
        advanceTimeBy(500)
        runCurrent()

        assertTrue(packets.isEmpty())
    }

    @Test
    fun GIVEN_empty_upstream_WHEN_collected_THEN_no_packets_emitted() = runTest {
        val packets = mutableListOf<ByteArray>()
        val sut = createSut(emptyFlow(), backgroundScope)

        backgroundScope.launch {
            sut.getPackets().collect { packets.add(it) }
        }
        runCurrent()
        advanceTimeBy(500)
        runCurrent()

        assertTrue(packets.isEmpty())
    }

    private fun createSut(
        source: Flow<ByteArray>,
        scope: CoroutineScope
    ) = FBleStatusStreamingApiImpl(
        subscribeFlow = source,
        scope = scope
    )

    private fun FBleStatusStreamingApiImpl.getPackets() = getEvents().map { event ->
        (event as StatusStreamingEvent.Protobuf).data
    }
}
