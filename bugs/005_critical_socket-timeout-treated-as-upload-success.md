# Socket timeout during firmware upload is silently treated as successful upload

## Type
broken-feature

**Severity:** critical

**Files:**
- `components/bridge/device/firmware-update/impl/src/commonMain/kotlin/net/flipper/bridge/device/firmwareupdate/uploader/api/FirmwareUploaderApiImpl.kt` (lines 56–72)

## Summary

`FirmwareUploaderApiImpl.uploadAndInstall` wraps the call to
`fFeatureApi.postUpdate(...)` in `try { ... } catch (_: SocketTimeoutException) { _state.emit(FirmwareUploaderState.Uploaded) }`.
Any socket timeout during the streaming upload — including timeouts that occur while only a fraction of the
firmware bytes have been pushed — is treated as a *successful upload* and unconditionally moves the state
machine to `Uploaded`, which then triggers `awaitDeviceReconnected()` and the install flow.

## Repro

1. Begin a LAN firmware upload of a multi-MB tgz to the BUSY Bar.
2. Pull the network cable / kill Wi-Fi / saturate the link mid-upload so that Ktor raises
   `io.ktor.client.network.sockets.SocketTimeoutException` while bytes are still in flight.
3. Observe that `FirmwareUploaderState` transitions to `Uploaded` even though only a partial firmware was
   delivered.
4. The orchestrator then proceeds with `awaitDeviceReconnected()` on the assumption that the install will run.

## Root cause

```kotlin
try {
    fFeatureApi.postUpdate(
        totalBytes = size,
        bytesFlow = SystemFileSystem.source(clientFilePath).asFlow(),
        onTransferred = { bytesUploaded -> /* progress */ }
    ).getOrThrow()
} catch (_: SocketTimeoutException) {
    info { "#uploadAndInstall device connection lost" }
    _state.emit(FirmwareUploaderState.Uploaded)   // <-- treats partial upload as success
}
```

The catch swallows the timeout silently and forces the state forward. There is no SHA verification against the
device, no re-check of `bytesUploaded == size`, and no path for the orchestrator to retry/resume.

## Impact

- A device that received only a partial firmware blob may attempt to install corrupt content. While the
  device-side updater should reject the SHA, status reporting in the SDK never sees the failure — the user is
  shown "Updating" / device-reconnect spinner indefinitely.
- The "device connection lost" comment suggests the original intent was to absorb the disconnect that happens
  *after* a successful `POST /api/update` triggers the device to reboot. That assumption is incorrect — the
  same exception type fires for any read timeout during the body stream.
- For consumer apps that rely on `FwUpdateState`, this is the difference between "firmware update silently
  failed" and "user-visible retry."

## Suggested fix

Distinguish "completed-then-disconnected" from "disconnected-mid-stream":

- Track `bytesUploaded` that has actually been written to `ByteWriteChannel` and only treat the timeout as a
  graceful disconnect when `bytesUploaded == totalBytes`.
- On any earlier timeout, emit `FirmwareUploaderState.Failed` (and surface it through the public
  `FwUpdateState`) so consumers can retry.
- Better: have `FRpcUpdaterApiImpl.postUpdate` confirm an HTTP 2xx (it currently doesn't read the body, see
  related issue) and only then resolve success. The Ktor body stream completing without a response is the real
  signal that the device accepted the update.
