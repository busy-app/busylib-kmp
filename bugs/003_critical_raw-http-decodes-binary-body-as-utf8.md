# critical — `toRawHttpRequestString` corrupts binary request bodies via `decodeToString()`

## Severity
critical

## Type
infrastructure

## Files
- `components/bridge/transport/common/impl/src/commonMain/kotlin/net/flipper/bridge/connection/transport/common/utils/HttpRequestRawKtx.kt:48-58` (the `StringBuilder` body append)
- Same file `:62-104` — `captureBodyBytes` / `OutgoingContent.toByteArray` machinery
- Call sites that ship the resulting `String` over a binary transport:
  - `components/bridge/transport/ble/impl/src/commonMain/kotlin/net/flipper/bridge/connection/transport/ble/impl/FHttpBLEEngine.kt:65`
  - `components/bridge/transport/mock/impl/src/commonMain/kotlin/net/flipper/bridge/connection/transport/mock/impl/BSBMockHttpEngine.kt:29`

## Summary
`toRawHttpRequestString` serialises an HTTP request to a single `String` and then is used by `FHttpBLEEngine` (and the mock engine) to send raw HTTP traffic to the device. The body is emitted by `sb.append(bytes.decodeToString())`. `ByteArray.decodeToString()` decodes the bytes as **UTF-8** and silently replaces every invalid UTF-8 sequence with the U+FFFD replacement character. Any non-text body — protobuf payloads, gzip-compressed JSON, image uploads, firmware blobs — is therefore mangled before it ever leaves the host. The downstream caller has no way to detect this; the request just produces a 4xx/5xx or, worse, a successful but garbled write to the device.

## Reproduction / scenario
1. Build any HTTP request with a binary body (e.g. `application/x-protobuf` carrying device-control bytes, or a multipart form with a non-ASCII byte).
2. Have it routed through `FHttpBLEEngine`/`BSBMockHttpEngine`, which call `processedRequest.toRawHttpRequestString()`.
3. Examine the resulting wire bytes: every byte ≥ 0x80 that is not part of a legal UTF-8 sequence has been replaced by `0xEF 0xBF 0xBD` (U+FFFD when re-encoded), and length no longer matches `Content-Length`.

## Why it happens
HTTP/1.1 framing is a **byte protocol**: the start line and headers are ASCII, but the body is opaque bytes. The author concatenated headers and body into a `String`, which forces the body through a UTF-8 decode/encode round trip. `decodeToString()`'s default behaviour replaces malformed sequences instead of throwing (`throwOnInvalidSequence = false`).

A second, related defect lives in the same path: `Content-Length` is set from `bytes.size` (the raw byte count) but the body is then injected as a *string*. If the caller re-encodes that string as UTF-8 (which Ktor will), high-bit bytes become 2- or 3-byte sequences and the wire body becomes longer than `Content-Length`, breaking framing.

## Impact
- Silent data corruption for any binary HTTP body sent over BLE / the mock engine.
- Misframed HTTP messages (Content-Length mismatch) → device may discard or hang on partial reads.
- Affects every feature that uploads firmware, images, audio, or protobuf via the HTTP-over-BLE bridge.

## Suggested fix
Stop returning a `String` for the wire format. Either:
1. Change the helper to produce `ByteArray` (or `Source`/`ByteReadChannel`) so the body bytes pass through untouched. Build the start line + headers as ASCII bytes, then concatenate `bytes` directly.
2. If the helper must return text for logging, split into two helpers: `toRawHttpRequestHeaders(): String` and `toRawHttpRequestBytes(): ByteArray`, and use the latter for transport.

Either way, audit `FHttpBLEEngine`/`BSBMockHttpEngine` to send the byte form and let `Content-Length` match the actual byte body.
