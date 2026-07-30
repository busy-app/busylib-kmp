package net.flipper.bridge.connection.screens.dashboard.drawtool

import kotlinx.io.files.Path
import net.flipper.bridge.connection.feature.provider.api.FFeatureProvider
import net.flipper.bridge.connection.feature.provider.api.getSync
import net.flipper.bridge.connection.feature.storage.api.FStorageFeatureApi
import net.flipper.core.busylib.ktx.io.FlipperFileSystem
import net.flipper.tools.drawtool.api.DrawToolStatusDirectoryLayout
import net.flipper.tools.drawtool.api.DrawToolStatusesApi
import net.flipper.tools.drawtool.layout.api.DefaultDrawToolStatusDirectoryLayout
import net.flipper.tools.drawtool.status.api.DefaultDrawToolStatusesApi
import net.flipper.tools.drawtool.status.util.DrawToolFileTypeResolver
import net.flipper.tools.drawtool.storage.api.DrawToolStoragePathProvider

/**
 * The directory this layout addresses: the parent every path it produces shares.
 *
 * A client collection is not keyed by bar serial, and the library names its root
 * only through the layout, so this is the one way to report the directory a
 * status was written into.
 */
private fun DrawToolStatusDirectoryLayout.collectionPath(): Path {
    return requireNotNull(getPreviewFilePath().parent) {
        "Draw tool layout produced a preview path without a directory"
    }
}

/**
 * Resolves the collection of a [DrawToolStorageTarget].
 *
 * The client side is fully owned by the library: it knows its own storage root,
 * so [clientStatusesApi] is asked for the layout instead of guessing it here.
 *
 * The bar side is assembled by hand on top of [FStorageFeatureApi], which is a
 * [FlipperFileSystem] like any other. Since [DefaultDrawToolStatusesApi] takes
 * both its filesystem and its root as parameters, the very reader the library
 * runs against the client works against the bar without a single change.
 */
class DrawToolCollectionSourceResolver(
    private val featureProvider: FFeatureProvider,
    private val clientStatusesApi: DrawToolStatusesApi,
    private val clientFileSystem: FlipperFileSystem,
    private val fileTypeResolver: DrawToolFileTypeResolver
) {
    private suspend fun resolveClient(): DrawToolCollectionSource {
        val layout = clientStatusesApi.getLayout().getOrThrow()
        return DrawToolCollectionSource(
            collectionPath = layout.collectionPath(),
            fileSystem = clientFileSystem,
            layout = layout,
            statusesApi = clientStatusesApi
        )
    }

    private suspend fun resolveBusyBar(): DrawToolCollectionSource {
        val storageFeatureApi = requireNotNull(featureProvider.getSync<FStorageFeatureApi>()) {
            "Storage feature is unavailable"
        }
        val collectionPath = DrawToolStatusDirectoryLayout.BUBSYBAR_DRAWTOOL_PATH
        return DrawToolCollectionSource(
            collectionPath = collectionPath,
            fileSystem = storageFeatureApi,
            layout = DefaultDrawToolStatusDirectoryLayout(collectionPath),
            statusesApi = DefaultDrawToolStatusesApi(
                drawToolStoragePathProvider = object : DrawToolStoragePathProvider {
                    override fun getPath(): Result<Path> = Result.success(collectionPath)
                },
                drawToolFileTypeResolver = fileTypeResolver,
                systemFileSystem = storageFeatureApi
            )
        )
    }

    suspend fun resolve(target: DrawToolStorageTarget): DrawToolCollectionSource {
        return when (target) {
            DrawToolStorageTarget.CLIENT -> resolveClient()
            DrawToolStorageTarget.BUSY_BAR -> resolveBusyBar()
        }
    }
}
