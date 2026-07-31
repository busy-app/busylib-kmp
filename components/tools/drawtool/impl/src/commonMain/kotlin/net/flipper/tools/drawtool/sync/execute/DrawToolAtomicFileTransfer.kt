package net.flipper.tools.drawtool.sync.execute

import dev.zacsweers.metro.Inject
import kotlinx.io.files.Path
import net.flipper.core.busylib.ktx.common.copyFileTo
import net.flipper.core.busylib.ktx.common.runSuspendCatching
import net.flipper.core.busylib.ktx.io.FlipperFileSystem

@Inject
class DrawToolAtomicFileTransfer {
    /**
     * Streams [sourcePath] into [temporaryPath] on [destination] and renames it
     * to [destinationPath] only whole — a crash never leaves a partial file
     * under the destination name.
     */
    suspend fun transfer(
        source: FlipperFileSystem,
        sourcePath: Path,
        destination: FlipperFileSystem,
        temporaryPath: Path,
        destinationPath: Path,
    ): Result<Unit> {
        return runSuspendCatching {
            source.copyFileTo(
                sourcePath = sourcePath,
                destinationPath = temporaryPath,
                destination = destination,
            )
            destination.atomicMove(temporaryPath, destinationPath)
        }
    }
}
