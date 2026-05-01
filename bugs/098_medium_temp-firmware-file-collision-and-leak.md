# Temp firmware file uses fixed name; concurrent or aborted downloads collide and leak the file on disk

## Type
broken-feature

**Severity:** medium

**Files:**
- `components/bridge/device/firmware-update/impl/src/commonMain/kotlin/net/flipper/bridge/device/firmwareupdate/downloader/api/FirmwareDownloaderApiImpl.kt` (lines 35–37, 67–110)

## Summary

```kotlin
private fun getTemporalPath(): Path {
    return Path(SystemTemporaryDirectory, "temp_firmware_update_file")
}
```

Two issues with this:

1. **Collisions across concurrent downloads.** Two concurrent `FirmwareDownloaderApiImpl` instances (e.g.
   one user has two BUSY Bars and the orchestrator triggers checks simultaneously) overwrite the same temp
   file mid-write. The SHA check then sees mixed content from two downloads and rejects both. Even single-
   instance, a retry while a previous download is still finishing flushes its bytes can corrupt the file.

2. **Leak on failure.** The file is `SystemFileSystem.sink(temporalFilePath, append = false).use { ... }`
   inside `prepareGet { ... }.execute { response -> ... }`. If the download fails (network error, scope
   cancellation, SHA mismatch), the file is **never deleted** — it sits in the temp directory until the OS
   cleans it (which on Android `cacheDir`/`SystemTemporaryDirectory` is rare and can take days). Multiple
   failed multi-MB attempts add up; on iOS it persists indefinitely because the temp directory is not
   purged by the OS unless the app explicitly does it.

3. **Ordering hazard with the upload step.** `FirmwareUpdaterApiImpl` calls
   `firmwareDownloaderApi.download(...).mapSuspendCatching { path -> firmwareUploaderApi.uploadAndInstall(path) { firmwareDownloaderApi.reset() } }`.
   The `reset()` lambda fires *after* the upload is requested but the file is still being read by
   `SystemFileSystem.source(clientFilePath)`. If a second `startUpdateInstall()` arrives concurrently it can
   try to download into the same path while the first is reading it — depending on the OS, the file is
   silently truncated and the upload streams garbage to the device.

## Repro

1. Start `startUpdateInstall()` for a Url-based update. While `firmwareUploaderApi.uploadAndInstall` is in
   flight, request a second `startUpdateInstall()` (via UI rapid-tap or external orchestrator retry).
2. The second download writes to the same `temp_firmware_update_file` while the first is reading from it.
   The first upload silently corrupts; SHA on device fails; user sees "DownloadFailure."
3. Or: trigger any download that fails midway. Verify the `temp_firmware_update_file` is left on disk.

## Root cause

Hard-coded temp filename and no `try { ... } finally { delete }` semantics around it.

## Impact

- Silent download corruption when retried quickly, leading to confusing "SHA mismatch" failures.
- Disk space leak on iOS / desktop.
- Cross-device contention if a single SDK instance is used to flash multiple BUSY Bars (which is supported by
  `MultiStreamApi` and other multi-device flows).

## Suggested fix

- Use a unique filename: `Path(SystemTemporaryDirectory, "bsb_firmware_${bsbUpdateVersion.sha256}.tgz")` and
  delete it in a `finally` block after upload completes.
- Wrap the entire `download → upload → cleanup` in a single try/finally that always removes the file.
- Add a one-time "vacuum" step on app launch that deletes any leftover `bsb_firmware_*` files from prior
  crashed sessions.
