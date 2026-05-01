# `rleDecompress` is unbounded in memory and crashes on malformed device frames

## Type
broken-feature

**Severity:** high

**Files:**
- `components/bridge/feature/screen-streaming/impl/src/commonMain/kotlin/net/flipper/bridge/connection/feature/screenstreaming/impl/delegates/RleDecompressKtx.kt` (lines 11–41)
- `components/bridge/feature/screen-streaming/impl/src/commonMain/kotlin/net/flipper/bridge/connection/feature/screenstreaming/impl/delegates/ScreenFramesProvider.kt` (lines 18–32)

## Summary

`rleDecompress` is the path that decodes RLE-compressed screen frames received from the device (over BLE, LAN,
or the Cloud WebSocket). It has two systemic problems:

1. **Boxed-byte memory blowup.** `decompressed` is a `mutableListOf<Byte>()`. Each `Byte` is boxed (typically
   16+ bytes on JVM), so a 72×16×3 = 3456-byte frame allocates ~55 KB of garbage. At 30 FPS over a long
   on-call session that is gigabytes of allocations and a hot path for the GC. On Native targets the boxing
   penalty is similar.
2. **No bounds checking.** `data[index + i]` and `data.copyOfRange(index, index + blkSize)` index past the
   end of `data` whenever the device sends a corrupted or maliciously short frame, throwing
   `ArrayIndexOutOfBoundsException`. `ScreenFramesProvider.mapFrame` then uses
   `runCatching { mapFrame(it) }.getOrNull()` to swallow the exception silently — the frame is dropped with
   no log line, no metric, and no path to surface "the device is sending us garbage" to the user.

There is also no upper bound on the resulting decompressed size: a 1-byte RLE control byte of `0x7F` followed
by `(0x7F * blkSize)` bytes of unique data is one thing; a `0x7F` repeat-count followed by `blkSize` bytes
*and* repeated, however, is bounded by the input length. But if `count` and `blkSize` are large, a single
control byte can ask for `count * blkSize` writes — repeated boxed-Byte additions add up fast. The current
implementation will happily allocate hundreds of MB if the device asks for it.

## Repro

1. Simulate a device that sends a `BusyLibUpdateEvent.Frame` with `encoding = RUN_LENGTH` and `data` that is
   shorter than the control bytes claim (e.g. `byteArrayOf(0x82, 0x00)` — claims 2 unique 3-byte blocks, only
   one byte of payload follows).
2. `rleDecompress` reads `data[index + i]` for `i in 0 until 6`, throws `ArrayIndexOutOfBoundsException`.
3. The frame is silently dropped; consumer sees nothing wrong; subsequent frames continue.

## Root cause

```kotlin
val decompressed = mutableListOf<Byte>()                 // boxed!
...
for (i in 0 until count * blkSize) {
    decompressed.add(data[index + i])                    // unchecked
}
index += count * blkSize                                 // can overshoot
...
val block = data.copyOfRange(index, index + blkSize)     // unchecked
```

## Impact

- Sustained GC pressure during streaming, especially on lower-end Android devices. Frame-rate drops + battery.
- Any malformed/truncated frame from the device crashes the decoder for that frame; with `runCatching` over
  it the user simply sees a stalled frame stream and has no diagnostic.
- The unbounded `count * blkSize` is a denial-of-service vector if the cloud / LAN path is ever exposed to
  hostile traffic — a single 2-byte payload can request ~127 × blkSize allocations.

## Suggested fix

```kotlin
fun rleDecompress(data: ByteArray, blkSize: Int): ByteArray {
    require(blkSize > 0)
    val out = ByteArray(estimateDecompressedSize(data, blkSize))   // pre-size
    var inIdx = 0
    var outIdx = 0
    while (inIdx < data.size) {
        val ctrlByte = data[inIdx].toInt() and 0xFF
        inIdx++
        if ((ctrlByte and 0x80) != 0) {
            val count = ctrlByte and 0x7F
            val bytes = count * blkSize
            require(inIdx + bytes <= data.size)
            data.copyInto(out, outIdx, inIdx, inIdx + bytes)
            inIdx += bytes
            outIdx += bytes
        } else {
            require(inIdx + blkSize <= data.size)
            repeat(ctrlByte) {
                data.copyInto(out, outIdx, inIdx, inIdx + blkSize)
                outIdx += blkSize
            }
            inIdx += blkSize
        }
    }
    return out.copyOf(outIdx)
}
```

…and replace the silent `runCatching { mapFrame(it) }.getOrNull()` in `ScreenFramesProvider` with at least
an `error(t) { "Failed to decode RLE frame, ${data.size} bytes" }` so we can see this in the wild.
