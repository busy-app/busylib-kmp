package net.flipper.tools.drawtool.storage.api

import dev.zacsweers.metro.Inject
import kotlinx.io.files.Path
import net.flipper.bsb.serial.storage.api.BsbDeviceSerialStore
import net.flipper.tools.drawtool.api.exception.DrawToolCollectionUnavailableException

/**
 * Resolves the collection for specific BUSY bar device
 *
 * Example: `/<android_app_path>/busylib/drawer/<BUSY bar serial number>/`
 */
@Inject
class DrawToolCollectionPathResolver(
    private val serialStore: BsbDeviceSerialStore,
    private val drawToolStoragePathProvider: DrawToolStoragePathProvider
) {
    private fun isSerialPathSafe(deviceSerial: String): Boolean {
        return deviceSerial.isNotBlank() &&
                deviceSerial.all { char ->
                    char.isLetterOrDigit() || char == '_' || char == '-'
                }
    }

    suspend fun getPath(uniqueId: String): Result<Path> = runCatching {
        val deviceSerial = serialStore.findSerial(uniqueId)
        return if (deviceSerial == null) {
            throw DrawToolCollectionUnavailableException(
                "Serial of the BUSY Bar '${uniqueId}' is not known yet; " +
                        "connect to the bar at least once"
            )
        } else if (!isSerialPathSafe(deviceSerial)) {
            throw DrawToolCollectionUnavailableException(
                "Device serial '$deviceSerial' cannot be used as a directory name"
            )
        } else {
            drawToolStoragePathProvider.getPath()
                .map { rootPath -> Path(rootPath, deviceSerial) }
        }
    }
}