# `forgetDevice` runs concurrently with the `connectionLoop`, can race the disconnect

## Type
infrastructure

**Severity:** medium

**Files:**
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/bridge/service/impl/src/commonMain/kotlin/net/flipper/bridge/connection/service/impl/FConnectionServiceImpl.kt` lines 106-150

## Summary

`forgetDevice` is a public `suspend` API that runs on the caller's coroutine
context, *not* on `singletonScope` (which is reserved for `connectionLoop`
and `forceRefreshConnection`). It performs:

1. A storage transaction that
   a. Looks up the device and the user principal (`first()`-style suspending
      collects from upstream flows).
   b. Calls the cloud bars REST API (network) — **inside** the storage mutex.
   c. Removes the device from storage.

Two real concerns:

1. **Network call inside storage `transactionInternal { ... }` blocks every
   other writer.** `transactionInternal` acquires `mutex` (see
   `FDevicePersistedStorageImpl.transactionInternal`). All watchers
   serialise on this mutex. A slow / hung HTTPS call (default Ktor timeout
   is generous) parks every storage write — including the connection
   loop's persisted-state reads in `getCurrentDeviceFlow()`.
2. **`forgetDevice` does not call `disconnectCurrent` / does not clear
   `currentSelectedDeviceId`.** The transaction `removeDevice(uniqueId)`
   should also reset the current pointer if it matches; the
   `PersistedStorageTransactionScopeImpl` impl does (see `removeDevice`
   contract in `DesktopHooksOrderTest.FakeTransactionScope`), but the
   public API contract is not stated. Meanwhile, `connectionLoop` may have
   already enqueued a `connectIfNot(currentDevice)` for the same device
   that is being forgotten. Once forget completes, `currentDevice` becomes
   null, but the orchestrator is still racing to *connect* to the device
   that was just forgotten — leading to a brief window where a "forgotten"
   device is actively being connected via cloud REST.

The worst variant is when the cloud unlink succeeds but the local
`removeDevice` is delayed by a second writer: the device is no longer in
the cloud, but still appears locally — and the connection loop tries to
reconnect via cloud, gets `404` from the cloud (which can manifest as auth
errors), and may wedge.

## Repro

1. User triggers forget. `forgetDevice` enters `transactionInternal`,
   acquires the storage mutex.
2. Before reaching `barsApi.unlinkBusyBar`, the cloud HTTPS layer hangs (TLS
   handshake stall, DNS retransmit). The storage mutex is now held for
   tens of seconds.
3. `BUSYLibNameWatcher`, `CloudFetcherWatcher`, `CloudProvisioningWatcher`,
   `HardwareIdProvisioningWatcher`, the connection loop's
   `getCurrentDeviceFlow()` reads — all park on the mutex.
4. The UI cannot navigate (storage reads via `getCurrentDeviceFlow()` are
   downstream of the mutex via `bleConfigKrate.flow`). Effective UI freeze.

## Root Cause

`forgetDevice` chose `transactionInternal` as the outer scope so the device
removal could be atomic with cloud unlink. That semantically conflates
"network" and "local persistence" lock domains.

## Impact

- During flaky cloud connectivity, the entire library can stall on a single
  `forgetDevice` call.
- `forgetDevice` is not cancellable in a clean way — cancellation while
  parked on the mutex leaves cloud-side state ambiguous (request may have
  completed server-side) but local storage unchanged.

## Suggested Fix

1. Restructure `forgetDevice`:
   - Do all network work *outside* the storage mutex (look up principal,
     call `barsApi.getBarsList`, call `barsApi.unlinkBusyBar`).
   - Only enter `transactionInternal` for the final `removeDevice(uniqueId)`
     step, after network operations either succeed or fail definitively.
2. Add an explicit `orchestrator.disconnectCurrent()` after a successful
   forget to ensure no in-flight reconnect attempt targets the forgotten
   device.
3. Add a test that holds the mutex for longer than a typical network timeout
   and verifies UI-relevant flows continue to emit.
