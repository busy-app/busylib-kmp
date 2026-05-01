# low — Cloud impl files live under `tcp.lan.impl` package — copy/paste leftover from LAN

## Severity
low

## Type
infrastructure

## Files
- All files under `components/bridge/transport/tcp/cloud/impl/src/commonMain/kotlin/net/flipper/bridge/connection/transport/tcp/lan/impl/...`

## Summary
The Cloud implementation module uses package path `net.flipper.bridge.connection.transport.tcp.lan.impl` — i.e. it pretends to be LAN. AGENTS.md says "Package naming mirrors module path: `net.flipper.bridge.connection.feature.<name>.{api,impl}`" — and even within this codebase the api module of cloud uses `tcp.cloud.api`. This is plainly wrong:

```
cloud/api/src/.../tcp/cloud/api/CloudDeviceConnectionApi.kt   ← correct
cloud/impl/src/.../tcp/lan/impl/CloudDeviceConnectionApiImpl.kt ← wrong, says "lan.impl"
cloud/impl/src/.../tcp/lan/impl/engine/BUSYCloudHttpEngine.kt   ← wrong
```

This is not a runtime bug, but:
- Causes confusion for future maintainers grepping for "where is cloud impl".
- Class names (`BUSYCloudHttpEngine`, `FCloudApiImpl`) clash visually with package (`tcp.lan.impl.engine`).
- Internal `internal typealias FCloudStreamingFactory` is in `tcp.lan.impl.metainfo` — won't be visible to anyone looking under `tcp.cloud.impl.metainfo`.

## Root Cause
Cloud module was clearly forked from LAN by copy-paste; package names were not renamed.

## Impact
- Maintainability / IDE navigation only.

## Suggested Fix
Bulk-rename `tcp.lan.impl` → `tcp.cloud.impl` in all cloud-impl source files; update imports across the project.
