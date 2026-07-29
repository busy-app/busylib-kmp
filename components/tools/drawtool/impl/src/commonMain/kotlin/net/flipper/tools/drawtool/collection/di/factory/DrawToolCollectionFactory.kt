package net.flipper.tools.drawtool.collection.di.factory

import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.io.files.Path
import kotlinx.serialization.json.Json
import net.flipper.core.busylib.data.di.qualifier.BusyLibJsonQualifier
import net.flipper.core.busylib.ktx.io.FlipperFileSystem
import net.flipper.tools.drawtool.collection.api.CollectionDrawToolStatusesApi
import net.flipper.tools.drawtool.collection.util.DrawToolFileTypeResolver
import net.flipper.tools.drawtool.collection.util.DrawToolStatusIdValidator
import net.flipper.tools.drawtool.collection.util.DrawToolStatusReader
import net.flipper.tools.drawtool.layout.api.DefaultDrawToolStatusDirectoryLayout

/**
 * Builds a [CollectionDrawToolStatusesApi] for a resolved collection directory.
 * The collection changes with the selected bar, so one is built per path, and
 * this is where everything derived from that path is wired: the directory
 * layout and the status reader.
 */
@AssistedInject
class DrawToolCollectionFactory(
    @Assisted private val fileSystem: FlipperFileSystem,
    private val fileTypeResolver: DrawToolFileTypeResolver,
    @BusyLibJsonQualifier private val json: Json,
    private val statusIdValidator: DrawToolStatusIdValidator
) {
    /** A collection rooted at [collectionPath], with its layout and reader wired. */
    fun create(collectionPath: Path): CollectionDrawToolStatusesApi {
        val layout = DefaultDrawToolStatusDirectoryLayout(collectionPath)
        return CollectionDrawToolStatusesApi(
            collectionPath = collectionPath,
            fileSystem = fileSystem,
            statusIdValidator = statusIdValidator,
            layout = layout,
            statusReader = DrawToolStatusReader(
                fileSystem = fileSystem,
                json = json,
                fileTypeResolver = fileTypeResolver,
                layout = layout
            )
        )
    }

    /**
     * Picks the filesystem the collections will sit on: local storage for a
     * client collection, the storage feature to work against the bar itself.
     */
    @AssistedFactory
    fun interface Factory {
        operator fun invoke(
            fileSystem: FlipperFileSystem
        ): DrawToolCollectionFactory
    }
}
