# WebSocket auth ticket and bearer tokens logged in plaintext at info/verbose

## Severity
critical

## Type
broken-feature

## Files
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/core/ktor/src/commonMain/kotlin/net/flipper/core/ktor/LoggingWebsocketConverter.kt` (lines 19-32)
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/core/ktor/src/commonMain/kotlin/net/flipper/core/ktor/HttpClient.kt` (lines 59-67)
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/core/ktor/src/commonMain/kotlin/net/flipper/core/ktor/util/KtorLoggerKtx.kt` (full file)
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/cloud/barsws/impl/src/commonMain/kotlin/net/flipper/bsb/cloud/barsws/api/utils/BSBWebSocket.kt` (line 74)

## Summary
Two log paths exfiltrate bearer / WS auth tickets at `info` (or `verbose`) — both gated
only by `BuildKonfig.IS_LOG_ENABLED` (NOT by `IS_SENSITIVE_LOG_ENABLED`):

1. **`LoggingWebsocketConverter.serialize`** logs the full WS frame text, including the
   first frame the SDK sends — `InternalWebSocketRequest.Authorization(token = …)` —
   to `verbose` via `verbose { ">>>>>>> $text" }`. Frame:
   `{"type":"authorization","token":"<TICKET>"}`.

2. **Ktor `Logging` plugin** is installed with `level = LogLevel.ALL`. It logs every
   request/response, including the `Authorization: Bearer <accessToken>` header on
   every REST call (`/api/v0/auth/ticket`, `/api/v0/bars/...`, `/api/v0/bars/{id}/access-token`)
   AND request/response bodies. The bodies of `BusyCloudAccessTokenResponse` (the
   long-lived access token), `BusyCloudTicketResponse.token` (the WS ticket), and
   `BSBApiPinRequest.pin` (the linking PIN) are all logged in plaintext. `minimizeBodyLogMessage`
   only filters `OctetStream`/image bodies — it does NOT redact `Authorization`
   headers or sensitive JSON fields.

3. **`BSBWebSocketImpl.send`** does `info { "Send $request" }` — for normal subscribe/
   unsubscribe requests this is harmless, but if a future `InternalWebSocketRequest`
   subtype carries a token it will leak there too.

The intentional `sensitive {}` channel exists (gated by `IS_SENSITIVE_LOG_ENABLED`
in `LogKtx.kt`), but neither the Ktor `Logging` plugin nor `LoggingWebsocketConverter`
route through it.

## Repro
1. Build with `BuildKonfig.IS_LOG_ENABLED = true` (the default for debug builds).
2. Sign in to the SDK, link a device, and let the WS connect.
3. Capture logcat / stdout. Search for `>>>>>>>`, `Authorization`, `Bearer`, `access_token`,
   `ticket`. The ticket token is on the first WS send, the bearer is on every REST call,
   and the access token is in the response body of the access-token endpoint.

## Root Cause
- `LoggingWebsocketConverter` was apparently designed for development debugging and was
  promoted to production without a redactor.
- Ktor `Logging` plugin defaults log everything at `LogLevel.ALL`; no header sanitizer
  is wired in.
- The reduction helper (`minimizeBodyLogMessage`) only handles binary content types.

## Impact
- A user who shares logs (bug report, support, telemetry exporter, file picker) can
  hand over a valid bearer access token (`expires_in = 600s` per
  `BusyCloudAccessTokenRequest`) AND a refreshable session.
- WS ticket has the same scope as the user session for the duration of its TTL.
- PINs leaked from `linkBusyBar` allow an attacker to claim a device that is currently
  in pairing mode.

## Suggested Fix
- For `LoggingWebsocketConverter`: route through `sensitive {}` rather than
  `verbose {}`, OR redact `"token":"…"` patterns before logging.
- For Ktor `Logging`:
  - Use `level = LogLevel.HEADERS` in production builds, or
  - Install a `sanitizeHeader { it.equals(HttpHeaders.Authorization, true) }` filter,
  - Extend `minimizeBodyLogMessage` to also redact JSON fields named `token`, `access_token`,
    `pin`, `password`, etc.
- For `BSBWebSocketImpl.send`, route through `sensitive {}` so an `Authorization`
  request type cannot accidentally leak.
