# `disconnectWifi()` treats "Already connected" as disconnect-success

## Severity
high

## Type
broken-feature

## Files
- `components/bridge/feature/rpc/impl/src/commonMain/kotlin/net/flipper/bridge/connection/feature/rpc/impl/exposed/FRpcWifiApiImpl.kt` (lines 41–53)
- `components/bridge/feature/rpc/api/src/commonMain/kotlin/net/flipper/bridge/connection/feature/rpc/api/model/BsbRpcError.kt`

## Summary
`disconnectWifi` swallows the response error `"Already connected"` and reports success. The constant used is `BsbRpcError.ALREADY_CONNECTED`, which is the error you would expect from `connectWifi` when the device is already connected — not from `disconnectWifi`. Either the constant name is wrong, or the wrong constant is being matched, or the firmware's idempotent-disconnect signal is something else entirely (e.g. `"Already disconnected"`/`"Not connected"`). In any of those cases, the public `disconnect(): CResult<Unit>` returns success on the wrong condition.

## Repro
1. Stub the device to return `ErrorResponse(error = "Already connected")` from `POST /api/wifi/disconnect`.
2. Call `FWiFiFeatureApi.disconnect()` → currently returns `CResult.Success(Unit)`.
3. Call it with the realistic firmware error (e.g. `"Already disconnected"`) → currently returns `CResult.Failure` because the string never matches.

```kotlin
override suspend fun disconnectWifi(): Result<SuccessResponse> {
    return runSuspendCatching(dispatcher) {
        val response = httpClient.post("/api/wifi/disconnect").body<ApiResponse>()
        return@runSuspendCatching when (response) {
            is ErrorResponse if response.error == BsbRpcError.ALREADY_CONNECTED.error -> {
                SuccessResponse(response.error)              // <-- wrong constant
            }
            is ErrorResponse -> error(response.error)
            is SuccessResponse -> response
        }
    }
}
```

## Root Cause
- `BsbRpcError` only declares `ALREADY_CONNECTED("Already connected")` and `ALREADY_LINKED("Already linked")` — no `ALREADY_DISCONNECTED`. The disconnect path was wired against the connect-time error string, almost certainly a copy-paste oversight from the connect implementation.

## Impact
- Real "already disconnected" responses surface as failures to consumers, who will retry/show errors unnecessarily.
- The dead branch could mask real `"Already connected"` errors if firmware ever returns one on `/disconnect` (e.g. on a wedged state machine), incorrectly reporting success.
- WiFi UI flows that rely on `disconnect().isSuccess` for state transitions become unreliable.

## Suggested Fix
- Add a dedicated enum entry, e.g. `ALREADY_DISCONNECTED("Already disconnected")` (or whatever the firmware actually returns; verify against bsb-firmware).
- Match it inside `disconnectWifi`, not `ALREADY_CONNECTED`.
- Add a unit test using a fake `HttpClient` that returns the firmware's idempotent-disconnect error and asserts `disconnectWifi()` returns success.
