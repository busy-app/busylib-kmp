package net.flipper.tools.drawtool.status.api

import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.busylib.core.wrapper.CResult
import net.flipper.busylib.core.wrapper.toCResult
import net.flipper.core.busylib.ktx.common.mapSuspendCatching
import net.flipper.core.busylib.ktx.common.transform
import net.flipper.core.busylib.ktx.io.SystemFlipperFileSystem
import net.flipper.tools.drawtool.api.DrawToolStatusDirectoryLayout
import net.flipper.tools.drawtool.api.DrawToolStatusesApi
import net.flipper.tools.drawtool.api.model.DrawToolStatus
import net.flipper.tools.drawtool.collection.di.factory.DrawToolCollectionFactory
import net.flipper.tools.drawtool.collection.util.DrawToolCollectionPathProvider
import net.flipper.tools.drawtool.collection.util.DrawToolStatusIdValidator
import net.flipper.tools.drawtool.layout.api.DefaultDrawToolStatusDirectoryLayout

/**
 * [DrawToolStatusesApi] over local storage. The collection is resolved per
 * operation, since the selected bar can change at any moment.
 *
 * One mutex serializes all writes: the crash-safe directory swap uses fixed
 * temporary names per status, so two writes of the same status must not
 * interleave. Reads take no lock.
 *
 * Single instance per graph — the mutex serializes nothing unless every caller
 * shares it.
 */
@SingleIn(BusyLibGraph::class)
@Inject
@ContributesBinding(BusyLibGraph::class, binding<DrawToolStatusesApi>())
class DefaultDrawToolStatusesApi(
    private val collectionResolver: DrawToolCollectionPathProvider,
    private val statusIdValidator: DrawToolStatusIdValidator,
    drawToolCollectionFactoryFactory: DrawToolCollectionFactory.Factory
) : DrawToolStatusesApi {
    private val writeMutex = Mutex()
    private val collectionFactory = drawToolCollectionFactoryFactory.invoke(
        fileSystem = SystemFlipperFileSystem(delegate = SystemFileSystem)
    )

    override suspend fun getStatusIds(uniqueId: String): CResult<ImmutableList<String>> {
        return collectionResolver.getPath(uniqueId).map(collectionFactory::create)
            .transform { collection -> collection.getStatusIds(uniqueId).toKotlinResult() }
            .toCResult()
    }

    override suspend fun getStatuses(uniqueId: String): CResult<ImmutableList<DrawToolStatus>> {
        return collectionResolver.getPath(uniqueId)
            .map(collectionFactory::create)
            .transform { collection -> collection.getStatuses(uniqueId).toKotlinResult() }
            .toCResult()
    }

    override suspend fun getStatus(
        uniqueId: String,
        statusId: String
    ): CResult<DrawToolStatus?> {
        return statusIdValidator.validate(statusId)
            .transform(collectionResolver::getPath)
            .map(collectionFactory::create)
            .transform { collection -> collection.getStatus(uniqueId, statusId).toKotlinResult() }
            .toCResult()
    }

    override suspend fun deleteStatuses(
        uniqueId: String,
        statusIds: ImmutableList<String>
    ): CResult<Unit> {
        statusIds.forEach { statusId ->
            statusIdValidator.validate(statusId)
                .onFailure { validationError -> return CResult.failure(validationError) }
        }
        return writeMutex.withLock {
            collectionResolver.getPath(uniqueId)
                .map(collectionFactory::create)
                .mapSuspendCatching { collection ->
                    collection.deleteStatuses(uniqueId, statusIds).getOrThrow()
                }
                .toCResult()
        }
    }

    override suspend fun getCollectionPath(uniqueId: String): CResult<Path> {
        return collectionResolver.getPath(uniqueId).toCResult()
    }

    override suspend fun getLayout(uniqueId: String): CResult<DrawToolStatusDirectoryLayout> {
        return collectionResolver.getPath(uniqueId)
            .map(::DefaultDrawToolStatusDirectoryLayout)
            .toCResult()
    }
}
