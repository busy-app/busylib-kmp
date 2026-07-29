package net.flipper.bridge.connection.feature.storage.api

import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.core.busylib.ktx.io.FlipperFileSystem

/**
 * The internal storage of the bar, seen as a filesystem.
 *
 * Being a [FlipperFileSystem] is the whole point of the feature: code that
 * already walks, copies and swaps files locally — the Draw tool sync above all
 * — works against the bar unchanged.
 *
 * Only paths under `/ext` exist, it being the single writable mount point of
 * the bar. Names may hold letters, digits, `.`, `_` and `-`; `.` and `..`
 * segments are rejected, because the bar normalizes nothing and would walk
 * straight out of `/ext`. Anything else fails with `kotlinx.io.IOException`.
 *
 * Every call costs an HTTP request, and the bar transfers whole files only —
 * no byte-range read, no append. So a source downloads the entire file on the
 * first read, a sink buffers everything in memory and uploads on
 * `flush`/`close`, and appending means download, concatenate, re-upload.
 * Memory use is proportional to file size; a sink is not a way to stream.
 */
interface FStorageFeatureApi : FDeviceFeatureApi, FlipperFileSystem
