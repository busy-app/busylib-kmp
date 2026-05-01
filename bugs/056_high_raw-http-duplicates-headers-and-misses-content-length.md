# high — `toRawHttpRequestString` duplicates headers and emits stale `Content-Length` when `includeBody = false`

## Severity
high

## Type
infrastructure

## Files
- `components/bridge/transport/common/impl/src/commonMain/kotlin/net/flipper/bridge/connection/transport/common/utils/HttpRequestRawKtx.kt:22-43`

## Summary
Two header-handling bugs in the same helper:

1. **Header duplication.** Lines 22-26 do
   ```kotlin
   val hb = HeadersBuilder().apply {
       appendAll(headers)
       appendAll(normalizedContent.headers)
       normalizedContent.contentType?.let { set(HttpHeaders.ContentType, it.toString()) }
   }
   ```
   `appendAll(headers)` followed by `appendAll(normalizedContent.headers)` does not de-duplicate. Any header present in both the user-provided `HttpRequestData.headers` and the body's `OutgoingContent.headers` (most commonly `Content-Type`, but also `Content-Encoding`, `Authorization` reissued by interceptors, `X-…` Ktor metadata) is emitted twice on the wire. Some receivers reject duplicate `Content-Type`; others pick the last; it is undefined behaviour.

2. **Stale `Content-Length` when stripping the body.** Lines 40-43:
   ```kotlin
   val bytes = if (includeBody) bodyBytes else ByteArray(0)
   if (includeBody && hb[HttpHeaders.ContentLength] == null) {
       hb[HttpHeaders.ContentLength] = bytes.size.toString()
   }
   ```
   When `includeBody = false`, the body is replaced with `ByteArray(0)` but the existing `Content-Length` header (whatever the user/content set, e.g. `4096`) is left untouched. The serialised request claims a body of N bytes that is not there, so any consumer that uses the helper for header-only transmission/logging will produce a request that is unparsable as HTTP.

## Reproduction / scenario
1. Construct a request whose body is `OutgoingContent.ByteArrayContent` carrying its own `Content-Type: application/json` header and the user passes `Content-Type: application/json` too. Result: two `Content-Type: application/json` lines in the raw request.
2. Or call `toRawHttpRequestString(includeBody = false)` on a POST with a 1 KB body; the produced raw bytes contain `Content-Length: 1024\r\n\r\n` followed by no body — invalid HTTP framing.

## Why it happens
- `Headers.appendAll` concatenates rather than merges/replaces, so duplicates are intentional behaviour of the API and must be normalised by the caller.
- The "strip body" branch was added to support log-only mode but the matching header normalisation was never performed.

## Impact
- Duplicate headers are rare in practice but can produce subtle, hard-to-debug request rejections from strict servers.
- The `includeBody = false` path produces malformed HTTP: any consumer using this string for sending (rather than logging) will lose the request silently.

## Suggested fix
- Replace `appendAll` chain with a deliberate merge: e.g. iterate `normalizedContent.headers.names()` and `set` (overwriting) into a copy of `headers` so each header has one canonical value.
- When `includeBody = false`: also `set(HttpHeaders.ContentLength, "0")` (or remove it) and strip `Transfer-Encoding`.
- Add a unit test that round-trips a request with overlapping headers and asserts each header appears once.
