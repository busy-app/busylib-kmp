# Smart Home mapper drops the `timestamp` field of the commissioning status

## Severity
medium

## Type
broken-feature

## Files
- `components/bridge/feature/smarthome/impl/src/commonMain/kotlin/net/flipper/bridge/connection/feature/smarthome/mapper/MatterCommissionedFabricsMapper.kt` (lines 10–20)
- `components/bridge/feature/smarthome/api/src/commonMain/kotlin/net/flipper/bridge/connection/feature/smarthome/model/BsbMatterCommissionedFabrics.kt`
- `components/bridge/feature/rpc/api/src/commonMain/kotlin/net/flipper/bridge/connection/feature/rpc/api/model/MatterCommissionedFabrics.kt`

## Summary
The RPC model `MatterCommissionedFabrics.CommissioningStatus` carries `timestamp: Instant?`, but `BsbMatterCommissionedFabrics.BsbCommissioningStatus` only declares `value: BsbCommissioningStatusType`. The mapper silently discards `timestamp`, so consumers can't tell *when* the latest pairing happened — a useful signal for "Recently paired" UI affordances and for diagnosing failed onboards.

## Repro
- Inspect the public model `BsbMatterCommissionedFabrics.BsbCommissioningStatus` — it has only `value`.
- Compare to the RPC source which has both `timestamp` and `value`.

## Root Cause
- The Bsb model omits the field; the mapper has nothing to map into.

## Impact
- Diagnostic info is lost.
- Consumers cannot implement "X seconds ago" semantics for the latest commissioning attempt.
- API gap: the bidirectional mapper (`toCommissioningStatus`) reconstructs the RPC model with `timestamp = null`, which means writing back-and-forth loses fidelity.

## Suggested Fix
- Add `val timestamp: Instant?` to `BsbMatterCommissionedFabrics.BsbCommissioningStatus`.
- Update both mappers to forward the field.
- Add a unit test that round-trips a non-null timestamp through the mapper.
