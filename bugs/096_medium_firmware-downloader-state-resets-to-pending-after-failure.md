# `FirmwareDownloaderApiImpl` resets state to `Pending` on failure, hiding error from public state

## Type
broken-feature

**Severity:** medium

**Files:**
- `components/bridge/device/firmware-update/impl/src/commonMain/kotlin/net/flipper/bridge/device/firmwareupdate/downloader/api/FirmwareDownloaderApiImpl.kt` (lines 67–110)
- `components/bridge/device/firmware-update/impl/src/commonMain/kotlin/net/flipper/bridge/device/firmwareupdate/downloader/model/FirmwareDownloaderState.kt`

## Summary

`FirmwareDownloaderState` is `Pending | Downloading | Downloaded` — there is no `Failed` variant. When
`download()` fails (network error, SHA mismatch, etc.) the implementation does:

```kotlin
.onFailure { t ->
    error(t) { "#downloadAndUpload could not finish download" }
    _state.emit(FirmwareDownloaderState.Pending)
}
```

This collapses every failure mode into the same `Pending` state that the downloader uses *before* a download
starts. `FwUpdateStatusMapper.toFwUpdateState(downloaderState=Pending, ...)` then emits
`FwUpdateState.UpdateAvailable` (because `bsbUrlUpdateVersion != null`) — i.e. the user is told "There is an
update, please install" *immediately after the install they just attempted blew up*. There is no observable
distinction between "user has not started download" and "download just failed."

Additionally, on `onCompletion` the `downloadIntoFile` flow emits `Downloaded` *unconditionally* when the
flow completes — including completion via cancellation, which is a separate hazard:

```kotlin
.onCompletion { _state.emit(FirmwareDownloaderState.Downloaded) }
```

If the calling coroutine is cancelled mid-download, `onCompletion` still emits `Downloaded`, so the parent
`startUpdateInstall()` flow then proceeds into the SHA check and upload phases with a partial file (the SHA
check at line 101 will catch the bad data, but the order of state emissions ends up `Downloading → Downloaded
→ Pending`, which is observable to subscribers).

## Repro

1. Start a `BsbUpdateVersion.Url` install. Disconnect Wi-Fi mid-download.
2. The `runSuspendCatching` catches the IO error and emits `Pending`. The public `FwUpdateState` immediately
   reports `UpdateAvailable`, no visible failure.
3. Or: cancel the parent scope mid-download. `onCompletion { ... Downloaded }` fires before the surrounding
   `runSuspendCatching` rewinds the state to `Pending`. Subscribers briefly see `FwUpdateState.Downloading
   (1f)` followed by `UpdateAvailable`.

## Root cause

`FirmwareDownloaderState` has no `Failed` state, and `onCompletion` is unconditional rather than
"on-non-error completion."

## Impact

- Users cannot distinguish "I never tried" from "I just failed." The retry button silently resubmits with no
  affordance for diagnosing the cause.
- Brief flicker of `Downloaded` on cancellation.

## Suggested fix

- Add `FirmwareDownloaderState.Failed(throwable)` and emit it from `onFailure`.
- Use `onCompletion { cause -> if (cause == null) _state.emit(Downloaded) }` so cancellation doesn't fake a
  successful download.
- Bubble the failure through `FwUpdateStatusMapper` to a real `FwUpdateState.DownloadFailure`.
