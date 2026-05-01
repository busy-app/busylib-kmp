# `BleStatusStreamingPacketParser` resets the assembly buffer on every out-of-order packet, including ones that follow a duplicate

## Type
broken-feature

**Severity:** low

**Files (lines):**
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/bridge/transport/ble/impl/src/commonMain/kotlin/net/flipper/bridge/connection/transport/ble/impl/streaming/BleStatusStreamingPacketParser.kt` (lines 16-46)

## Summary

```kotlin
fun consume(chunk: ByteArray): ByteArray? {
    val header = chunk.parseHeaderOrNull() ?: run { reset(); return null }
    val isStartChunk = header.num == 0
    val hasActiveAssembly = expectedCount != null

    if (!hasActiveAssembly) {
        if (!isStartChunk) return null
        expectedCount = header.count
        expectedNextNum = 0
    } else if (header.count != expectedCount || header.num != expectedNextNum) {
        reset()
        if (!isStartChunk) return null
        expectedCount = header.count
        expectedNextNum = 0
    }

    val payloadChunk = chunk.copyOfRange(HEADER_SIZE, HEADER_SIZE + header.size)
    chunks.add(payloadChunk)
    totalPayloadSize += payloadChunk.size
    expectedNextNum += 1

    if (header.num != header.count - 1) return null
    return assemblePayload().also { reset() }
}
```

Edge cases:

1. **Duplicate packet (same num).** A duplicate (num=k arrives twice while
   assembling) is treated as out-of-order. We `reset()` and then check
   `isStartChunk`. If the duplicate is not start, we silently drop the
   in-progress frame and lose all collected chunks. BLE notifications can
   legitimately arrive duplicated when the stack retransmits.
2. **Header validation does not enforce `count` vs `expectedCount`
   monotonicity** — we accept any `count` as the start of a new frame.
   If a stream chunk `num=0` of a different `count` interleaves with a
   slow assembly, we drop the in-progress frame.
3. The parser is **stateful and not thread-safe**. It is used inside a
   `flow { … collect { … parser.consume(…) } }` block which is sequential
   per-collector, so today this is OK; but if `reassembleByHeader()`
   is ever shared across collectors via `shareIn`, the parser races.

## Reproduction

Send a status streaming sequence `[0/3, 1/3, 1/3, 2/3]` (duplicate `1/3`
because the BLE stack retransmitted): the parser drops the frame and
returns nothing. The user sees the BLE status stream dropping legitimate
events.

## Root cause

Single-resync strategy with no tolerance for retransmission.

## Impact

- Sporadic loss of status events under unreliable BLE conditions.

## Suggested fix

- Track last-seen `num`; if `header.num == expectedNextNum - 1`, treat as
  a duplicate and drop the chunk *without* resetting the assembly buffer.
- Add a `parserStartedAt` timestamp; if assembly is older than e.g. 1 s,
  reset on a new `num=0` even if `count` differs.
- Document thread-safety constraints on the class header.
