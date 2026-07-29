package net.flipper.tools.drawtool.collection.util

import dev.zacsweers.metro.Inject
import kotlinx.io.files.Path
import net.flipper.bsb.serial.storage.api.BsbDeviceSerialStore
import net.flipper.core.busylib.ktx.common.runSuspendCatching
import net.flipper.core.busylib.ktx.common.transform
import net.flipper.tools.drawtool.api.exception.DrawToolCollectionUnavailableException
import net.flipper.tools.drawtool.storage.api.DrawToolStoragePathProvider

/**
 * Collection directory of one BUSY Bar, named by its serial number — so it has
 * to be a serial the filesystem accepts as a directory name.
 *
 * Example: `/<android_app_path>/busylib/drawer/<BUSY bar serial number>/`
 */
@Inject
class DrawToolCollectionPathProvider(
    private val serialStore: BsbDeviceSerialStore,
    private val drawToolStoragePathProvider: DrawToolStoragePathProvider
) {
    private fun isSerialPathSafe(deviceSerial: String): Boolean {
        return deviceSerial.isNotBlank() &&
            deviceSerial.all { char ->
                char.isLetterOrDigit() || char == '_' || char == '-'
            }
    }

    private fun validateDeviceSerial(
        uniqueId: String,
        deviceSerial: String?
    ): Result<String> {
        return when {
            deviceSerial == null -> Result.failure(
                DrawToolCollectionUnavailableException(
                    "Serial of the BUSY Bar '$uniqueId' is not known yet; " +
                        "connect to the bar at least once"
                )
            )

            !isSerialPathSafe(deviceSerial) -> Result.failure(
                DrawToolCollectionUnavailableException(
                    "Device serial '$deviceSerial' cannot be used as a directory name"
                )
            )

            else -> Result.success(deviceSerial)
        }
    }

    /**
     * Collection directory of the bar [uniqueId], or
     * [DrawToolCollectionUnavailableException] while its serial is unknown or
     * cannot be used as a directory name.
     */
    suspend fun getPath(uniqueId: String): Result<Path> {
        return runSuspendCatching { serialStore.findSerial(uniqueId) }
            .transform { deviceSerial -> validateDeviceSerial(uniqueId, deviceSerial) }
            .transform { deviceSerial ->
                drawToolStoragePathProvider.getPath()
                    .map { rootPath -> Path(rootPath, deviceSerial) }
            }
    }
}
