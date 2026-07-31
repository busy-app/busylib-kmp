package net.flipper.core.busylib.data.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.serialization.json.Json
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.core.busylib.data.di.qualifier.BusyLibJsonQualifier

@ContributesTo(BusyLibGraph::class)
@BindingContainer
object BusyLibJsonModule {

    @Provides
    @SingleIn(BusyLibGraph::class)
    @BusyLibJsonQualifier
    fun provideBusyLibJson(): Json {
        return Json {
            isLenient = true
            ignoreUnknownKeys = true
            encodeDefaults = true
            prettyPrint = false
        }
    }
}
