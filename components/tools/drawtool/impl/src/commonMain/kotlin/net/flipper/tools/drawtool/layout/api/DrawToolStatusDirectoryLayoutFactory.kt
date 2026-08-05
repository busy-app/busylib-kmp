package net.flipper.tools.drawtool.layout.api

import dev.zacsweers.metro.Inject
import kotlinx.io.files.Path
import net.flipper.tools.drawtool.api.DrawToolStatusDirectoryLayout

@Inject
class DrawToolStatusDirectoryLayoutFactory {

    fun createLocalLayout(collectionPath: Path): DrawToolStatusDirectoryLayout {
        return DefaultDrawToolStatusDirectoryLayout(collectionPath)
    }

    fun createBarLayout(): DrawToolStatusDirectoryLayout {
        return createLocalLayout(DrawToolStatusDirectoryLayout.BUSYBAR_DRAWTOOL_PATH)
    }
}
