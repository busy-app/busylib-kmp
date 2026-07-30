package net.flipper.bridge.connection.screens.dashboard.drawtool

import kotlinx.serialization.json.Json
import net.flipper.bridge.connection.feature.provider.api.FFeatureProvider
import net.flipper.bridge.connection.feature.provider.api.getSync
import net.flipper.bridge.connection.feature.storage.api.FStorageFeatureApi
import net.flipper.core.busylib.ktx.io.FlipperFileSystem
import net.flipper.tools.drawtool.api.DrawToolStatusDirectoryLayout
import net.flipper.tools.drawtool.api.DrawToolStatusesApi
import net.flipper.tools.drawtool.collection.api.CollectionDrawToolStatusesApi
import net.flipper.tools.drawtool.collection.util.DrawToolStatusIdValidator
import net.flipper.tools.drawtool.collection.util.DrawToolStatusReader
import net.flipper.tools.drawtool.layout.api.DefaultDrawToolStatusDirectoryLayout
import net.flipper.tools.drawtool.status.util.DrawToolFileTypeResolver

/**
 * Resolves the collection of a [DrawToolStorageTarget].
 *
 * The client side is fully owned by the library: it knows the bar serial and
 * therefore the collection directory, so [clientStatusesApi] is asked for both
 * the path and the layout instead of guessing them here.
 *
 * The bar side is assembled by hand on top of [FStorageFeatureApi], which is a
 * [FlipperFileSystem] like any other. That is exactly what makes the same
 * collection reader work against the bar without a single change.
 */
class DrawToolCollectionSourceResolver(
    private val featureProvider: FFeatureProvider,
    private val clientStatusesApi: DrawToolStatusesApi,
    private val clientFileSystem: FlipperFileSystem,
    private val json: Json,
    private val statusIdValidator: DrawToolStatusIdValidator,
    private val fileTypeResolver: DrawToolFileTypeResolver
) {
    private suspend fun resolveClient(uniqueId: String): DrawToolCollectionSource {
        return DrawToolCollectionSource(
            collectionPath = clientStatusesApi.getCollectionPath(uniqueId).getOrThrow(),
            fileSystem = clientFileSystem,
            layout = clientStatusesApi.getLayout(uniqueId).getOrThrow(),
            statusesApi = clientStatusesApi
        )
    }

    private suspend fun resolveBusyBar(): DrawToolCollectionSource {
        val storageFeatureApi = requireNotNull(featureProvider.getSync<FStorageFeatureApi>()) {
            "Storage feature is unavailable"
        }
        val collectionPath = DrawToolStatusDirectoryLayout.BUBSYBAR_DRAWTOOL_PATH
        val layout = DefaultDrawToolStatusDirectoryLayout(collectionPath)
        return DrawToolCollectionSource(
            collectionPath = collectionPath,
            fileSystem = storageFeatureApi,
            layout = layout,
            statusesApi = CollectionDrawToolStatusesApi(
                collectionPath = collectionPath,
                fileSystem = storageFeatureApi,
                statusIdValidator = statusIdValidator,
                layout = layout,
                statusReader = DrawToolStatusReader(
                    fileSystem = storageFeatureApi,
                    json = json,
                    fileTypeResolver = fileTypeResolver,
                    layout = layout
                )
            )
        )
    }

    suspend fun resolve(
        target: DrawToolStorageTarget,
        uniqueId: String
    ): DrawToolCollectionSource {
        return when (target) {
            DrawToolStorageTarget.CLIENT -> resolveClient(uniqueId)
            DrawToolStorageTarget.BUSY_BAR -> resolveBusyBar()
        }
    }
}
