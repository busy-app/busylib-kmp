package net.flipper.bridge.connection.transport.ble.impl.streaming

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.flipper.bridge.connection.transport.common.api.serial.StatusStreamingEvent
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FBleStatusStreamingApiImplTest {

    // Single-chunk message (num=0, count=1) → emit immediately
    @Test
    fun GIVEN_single_chunk_message_WHEN_received_THEN_payload_emitted() = runTest {
        val source = MutableSharedFlow<ByteArray>()
        val packets = mutableListOf<ByteArray>()
        val sut = createSut(source, backgroundScope)

        backgroundScope.launch { sut.getPackets().collect { packets.add(it) } }
        runCurrent()

        source.emit(makeChunk(num = 0, count = 1, payload = byteArrayOf(1, 2, 3)))
        runCurrent()

        assertEquals(1, packets.size)
        assertContentEquals(byteArrayOf(1, 2, 3), packets.single())
    }

    // Multi-chunk message → emit only after last chunk
    @Test
    fun GIVEN_multi_chunk_message_WHEN_all_chunks_received_THEN_reassembled_payload_emitted() = runTest {
        val source = MutableSharedFlow<ByteArray>()
        val packets = mutableListOf<ByteArray>()
        val sut = createSut(source, backgroundScope)

        backgroundScope.launch { sut.getPackets().collect { packets.add(it) } }
        runCurrent()

        source.emit(makeChunk(num = 0, count = 3, payload = byteArrayOf(1, 2)))
        runCurrent()
        assertTrue(packets.isEmpty())

        source.emit(makeChunk(num = 1, count = 3, payload = byteArrayOf(3, 4)))
        runCurrent()
        assertTrue(packets.isEmpty())

        source.emit(makeChunk(num = 2, count = 3, payload = byteArrayOf(5, 6)))
        runCurrent()

        assertEquals(1, packets.size)
        assertContentEquals(byteArrayOf(1, 2, 3, 4, 5, 6), packets.single())
    }

    // Two consecutive messages → two separate packets
    @Test
    fun GIVEN_two_separate_messages_WHEN_received_THEN_two_packets_emitted() = runTest {
        val source = MutableSharedFlow<ByteArray>()
        val packets = mutableListOf<ByteArray>()
        val sut = createSut(source, backgroundScope)

        backgroundScope.launch { sut.getPackets().collect { packets.add(it) } }
        runCurrent()

        source.emit(makeChunk(num = 0, count = 1, payload = byteArrayOf(10, 20)))
        runCurrent()
        source.emit(makeChunk(num = 0, count = 1, payload = byteArrayOf(30, 40)))
        runCurrent()

        assertEquals(2, packets.size)
        assertContentEquals(byteArrayOf(10, 20), packets[0])
        assertContentEquals(byteArrayOf(30, 40), packets[1])
    }

    // Chunk shorter than header → ignored
    @Test
    fun GIVEN_chunk_shorter_than_header_WHEN_received_THEN_ignored() = runTest {
        val source = MutableSharedFlow<ByteArray>()
        val packets = mutableListOf<ByteArray>()
        val sut = createSut(source, backgroundScope)

        backgroundScope.launch { sut.getPackets().collect { packets.add(it) } }
        runCurrent()

        source.emit(byteArrayOf(0, 0, 0))
        runCurrent()

        assertTrue(packets.isEmpty())
    }

    // count == 0 in header → ignored
    @Test
    fun GIVEN_chunk_with_zero_count_WHEN_received_THEN_ignored() = runTest {
        val source = MutableSharedFlow<ByteArray>()
        val packets = mutableListOf<ByteArray>()
        val sut = createSut(source, backgroundScope)

        backgroundScope.launch { sut.getPackets().collect { packets.add(it) } }
        runCurrent()

        source.emit(makeChunk(num = 0, count = 0, payload = byteArrayOf(1, 2)))
        runCurrent()

        assertTrue(packets.isEmpty())
    }

    // size field exceeds actual data → ignored
    @Test
    fun GIVEN_chunk_with_size_exceeding_data_WHEN_received_THEN_ignored() = runTest {
        val source = MutableSharedFlow<ByteArray>()
        val packets = mutableListOf<ByteArray>()
        val sut = createSut(source, backgroundScope)

        backgroundScope.launch { sut.getPackets().collect { packets.add(it) } }
        runCurrent()

        // header claims size=10 but only 2 bytes of payload follow
        val bad = makeChunkWithOverriddenSize(num = 0, count = 1, declaredSize = 10, payload = byteArrayOf(1, 2))
        source.emit(bad)
        runCurrent()

        assertTrue(packets.isEmpty())
    }

    // Empty upstream → no packets
    @Test
    fun GIVEN_empty_upstream_WHEN_collected_THEN_no_packets_emitted() = runTest {
        val packets = mutableListOf<ByteArray>()
        val sut = createSut(emptyFlow(), backgroundScope)

        backgroundScope.launch { sut.getPackets().collect { packets.add(it) } }
        runCurrent()

        assertTrue(packets.isEmpty())
    }

    // Flow completion mid-message → no partial packet emitted
    @Test
    fun GIVEN_incomplete_message_WHEN_upstream_completes_THEN_no_partial_packet_emitted() = runTest {
        val sut = createSut(
            source = flow {
                emit(makeChunk(num = 0, count = 2, payload = byteArrayOf(1, 2)))
                // second chunk never arrives
            },
            scope = backgroundScope
        )

        val packets = mutableListOf<ByteArray>()
        backgroundScope.launch { sut.getPackets().collect { packets.add(it) } }
        runCurrent()

        assertTrue(packets.isEmpty())
    }

    // Complete message via flow completion → emitted
    @Test
    fun GIVEN_complete_message_in_finite_flow_WHEN_collected_THEN_packet_emitted() = runTest {
        val sut = createSut(
            source = flow {
                emit(makeChunk(num = 0, count = 2, payload = byteArrayOf(1, 2)))
                emit(makeChunk(num = 1, count = 2, payload = byteArrayOf(3, 4)))
            },
            scope = backgroundScope
        )

        val packet = sut.getPackets().first()
        assertContentEquals(byteArrayOf(1, 2, 3, 4), packet)
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private fun makeChunk(num: Int, count: Int, payload: ByteArray): ByteArray =
        byteArrayOf(
            (num and 0xFF).toByte(),
            ((num shr 8) and 0xFF).toByte(),
            (count and 0xFF).toByte(),
            ((count shr 8) and 0xFF).toByte(),
            (payload.size and 0xFF).toByte(),
            ((payload.size shr 8) and 0xFF).toByte()
        ) + payload

    private fun makeChunkWithOverriddenSize(
        num: Int,
        count: Int,
        declaredSize: Int,
        payload: ByteArray
    ): ByteArray = byteArrayOf(
        (num and 0xFF).toByte(),
        ((num shr 8) and 0xFF).toByte(),
        (count and 0xFF).toByte(),
        ((count shr 8) and 0xFF).toByte(),
        (declaredSize and 0xFF).toByte(),
        ((declaredSize shr 8) and 0xFF).toByte()
    ) + payload

    private fun createSut(source: Flow<ByteArray>, scope: CoroutineScope) =
        FBleStatusStreamingApiImpl(subscribeFlow = source, scope = scope)

    private fun FBleStatusStreamingApiImpl.getPackets(): Flow<ByteArray> =
        getEvents().map { (it as StatusStreamingEvent.Protobuf).data }
}
