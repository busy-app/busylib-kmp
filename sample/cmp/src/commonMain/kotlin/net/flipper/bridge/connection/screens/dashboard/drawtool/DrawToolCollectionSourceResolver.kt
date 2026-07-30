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
import net.flipper.tools.drawtool.status.util.DrawToolStoredFileResolver
import net.flipper.tools.drawtool.storage.api.DrawToolStoragePathProvider

/**
 * The directory of this layout: the parent every path it produces shares. The
 * library names its client root only through the layout, so this is the one way
 * to learn it.
 */
private fun DrawToolStatusDirectoryLayout.collectionPath(): Path {
    return requireNotNull(getPreviewFilePath().parent) {
        "Draw tool layout produced a preview path without a directory"
    }
}

/**
 * Resolves the collection of a [DrawToolStorageTarget].
 *
 * The bar side is assembled by hand on top of [FStorageFeatureApi], a
 * [FlipperFileSystem] like any other: [DefaultDrawToolStatusesApi] takes both
 * its filesystem and its root, so the client reader works against the bar
 * unchanged. Only its collection half is used — `upload*` and `show*` on a bar
 * backed instance would copy the bar onto itself.
 */
class DrawToolCollectionSourceResolver(
    private val featureProvider: FFeatureProvider,
    private val clientStatusesApi: DrawToolStatusesApi,
    private val clientFileSystem: FlipperFileSystem,
    private val storedFileResolver: DrawToolStoredFileResolver
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
        val collectionPath = DrawToolStatusDirectoryLayout.BUSYBAR_DRAWTOOL_PATH
        return DrawToolCollectionSource(
            collectionPath = collectionPath,
            fileSystem = storageFeatureApi,
            layout = DefaultDrawToolStatusDirectoryLayout(collectionPath),
            statusesApi = DefaultDrawToolStatusesApi(
                drawToolStoragePathProvider = object : DrawToolStoragePathProvider {
                    override fun getPath(): Result<Path> = Result.success(collectionPath)
                },
                drawToolStoredFileResolver = storedFileResolver,
                featureProvider = featureProvider,
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
