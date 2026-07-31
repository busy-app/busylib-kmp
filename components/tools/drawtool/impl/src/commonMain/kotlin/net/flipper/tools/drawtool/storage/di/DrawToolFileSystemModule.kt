package net.flipper.tools.drawtool.storage.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.io.files.SystemFileSystem
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.core.busylib.ktx.io.FlipperFileSystem
import net.flipper.core.busylib.ktx.io.SystemFlipperFileSystem

/**
 * The local filesystem of the client device. Provided from the graph so no
 * class constructs its own copy — the bar filesystem, in contrast, is a
 * connection feature and never lives in the graph.
 */
@ContributesTo(BusyLibGraph::class)
@BindingContainer
object DrawToolFileSystemModule {

    @Provides
    @SingleIn(BusyLibGraph::class)
    @ClientFileSystemQualifier
    fun provideSystemFlipperFileSystem(): FlipperFileSystem {
        return SystemFlipperFileSystem(SystemFileSystem)
    }
}
