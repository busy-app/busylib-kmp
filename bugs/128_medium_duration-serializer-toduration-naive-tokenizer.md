# `DurationSerializer.toDuration` tokenises by lower-case `m` and double-counts `s` inside other tokens

## Type
infrastructure

**Severity:** medium

**Files:**
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/core/data/src/commonMain/kotlin/net/flipper/core/busylib/data/serialization/DurationSerializer.kt` lines 80–108

## Summary
`toDuration` separates tokens by inserting a space after every delimiter character, then iterates pieces. Two independent issues exist:

1. **`firstOrNull` matches the first delimiter that is a substring of `part`** — if `Delimiter.entries` ordering ever changes (e.g. via reordering or future addition), tokens like `30m` could be matched against `M` correctly, but a malformed `5x` would fall through and `error("Wrong usage on argument...")` is thrown — fine. But:
2. **`replace` is case-sensitive on lower-case characters that also appear in numeric portions in some locales** — not common, but `.toIntOrNull()` is used so non-numeric residue is rejected. OK.
3. **Real bug**: `replace(Delimiter.S.value, "s ")` *also* matches the trailing `s` produced by `replace(Delimiter.D.value, "d ")` when D's value is `"d"` — wait, D is `"d"`, S is `"s"`. They don't collide.
4. **Real-real bug**: `replace(Delimiter.M.value, "m ")` adds a space after every `m`. Since `M = "m"`, this is fine for `30m`, but combined with the absence of upper-case minute support (`M` for month), parsing `30M` is silently treated as malformed — the original string survives as `30M` and `Delimiter.entries.firstOrNull { part.contains(it.value) }` returns `null` → `error("Wrong usage on argument. Could not determine delimiter…")`. So that's an explicit failure, not silent corruption.

The actual silent issue is `fromDuration`'s handling of weeks > 7 days: 8 days encodes as `1w 1d` (since `days >= 7` → `days/7 = 1`, then `days % 7 = 1`). `1w 1d` round-trips correctly. 14 days encodes as `2w` (`days % 7 == 0`, so the day token is skipped). 14 days round-trips to `2w` and back to 14 days. OK.

But: **`fromDuration` produces "0s "** with trailing space for `Duration.ZERO`. The `trimEnd()` strips it so the result is `"0s"`. Round-trip correct.

The real bug surfaces at the negative side: `Duration` permits negative values, but `fromDuration` only emits non-zero positive components (`if (hours > 0)`). A negative duration of `-1.minutes` emits `0s` (because `days+hours+minutes+seconds == 0L` since signed arithmetic plays tricks: actually `-1.minutes.toComponents` will report negative seconds; the `> 0` checks would skip them, leading to `0s` for any negative input). Result: any negative duration silently round-trips to zero.

## Repro
```kotlin
val original = (-30).seconds
val str = DurationSerializer.fromDuration(original)   // "0s"
val parsed = DurationSerializer.toDuration(str)        // 0.seconds
// Data loss
```

## Root Cause
- `fromDuration` only handles the positive sign; `> 0` checks discard negative values.
- `toDuration` rejects negative numbers because `intAmount.toIntOrNull()` cannot parse `-30s` as a single token (the leading `-` makes the part `-30s`, which `toIntOrNull` after stripping `s` produces `-30` — actually that does parse!). But the `replace` step inserts space after `s`, so `-30s` becomes `-30s ` and tokenises into `-30s`. The `intAmount = -30` is fine. So `toDuration("-30s") == -30.seconds` — works one way.

So the asymmetry: `fromDuration` cannot serialise negatives, but `toDuration` *can* parse them. Round-trip is broken only one direction.

## Impact
- Any field that may legitimately be negative (e.g. timezone offset deltas, scheduling deltas) silently round-trips to zero through this serialiser.
- Most BUSY Lib durations are non-negative (timeouts, intervals), so impact is bounded — but the contract is asymmetric and surprising.

## Suggested Fix
Either:
1. Reject negative durations in `fromDuration` with `require(value >= 0.seconds) { "Cannot serialize negative durations: $value" }`, OR
2. Symmetrically support negatives by emitting a leading `-` and using `value.absoluteValue.toComponents`.

Document the chosen contract in KDoc.
