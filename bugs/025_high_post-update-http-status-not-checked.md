# `postUpdate` does not check HTTP response status, declares success on any response

## Type
broken-feature

**Severity:** high

**Files:**
- `components/bridge/feature/rpc/impl/src/commonMain/kotlin/net/flipper/bridge/connection/feature/rpc/impl/exposed/FRpcUpdaterApiImpl.kt` (lines 94–120)

## Summary

`FRpcUpdaterApiImpl.postUpdate` posts the firmware tgz to `/api/update` and discards the `HttpResponse`
without inspecting the status or body. Unless the Ktor client is constructed with `expectSuccess = true`,
the function returns `Result.success(Unit)` even on `400 Bad Request` (e.g. SHA mismatch or "wrong target")
returned by the device. Even with `expectSuccess`, the response body is never consumed, so any structured
error payload is lost.

## Repro

1. Configure a malformed update file (e.g. wrong SHA256 / corrupted tgz).
2. Call `postUpdate(...)`. The device-side updater returns 4xx with an error message.
3. `FRpcUpdaterApiImpl.postUpdate` returns `Result.success(Unit)`.
4. `FirmwareUploaderApiImpl` proceeds to emit `Uploaded` and the orchestrator advances to the install/awaiting
   reconnect phase, even though no install is happening on the device.

## Root cause

```kotlin
override suspend fun postUpdate(...) : Result<Unit> {
    return runSuspendCatching(dispatcher) {
        httpClient.post("/api/update") {
            contentType(ContentType.Application.OctetStream)
            setBody(object : OutgoingContent.WriteChannelContent() { ... })
        }
        // <-- HttpResponse is never checked, body never read
    }.map { }
}
```

Compare with sibling endpoints in the same file (`startUpdateInstall`, `setAutoUpdate`) which all do
`.body<SuccessResponse>()` and would surface server-side errors.

## Impact

- Any device-side error during upload acceptance is invisible to the SDK.
- The firmware update state machine moves forward on a non-event, leading to "stuck waiting for reboot"
  states that the user cannot resolve except by force-quitting the app.

## Suggested fix

```kotlin
val response = httpClient.post("/api/update") { ... }
if (!response.status.isSuccess()) {
    error("postUpdate failed: ${response.status} ${response.bodyAsText()}")
}
// or, parse it like sibling calls
response.body<SuccessResponse>()
```

Have `FirmwareUploaderApiImpl` rely on the *successful return* of `postUpdate` rather than on a swallowed
`SocketTimeoutException` to advance state.
