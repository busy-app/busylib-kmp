package net.flipper.busylib.di

import me.tatarka.inject.annotations.IntoMap
import me.tatarka.inject.annotations.Provides
import net.flipper.bridge.connection.transport.common.api.DeviceConnectionApiHolder
import net.flipper.bridge.connection.transport.mock.FMockDeviceConnectionConfig
import net.flipper.bridge.connection.transport.mock.MockDeviceConnectionApi
import net.flipper.busylib.core.di.BusyLibGraph
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo
import kotlin.reflect.KClass

/**
 * Workaround for kotlin-inject-anvil limitation in Kotlin Multiplatform projects.
 *
 * ## Why this exists:
 * In Kotlin Multiplatform, kotlin-inject-anvil has a known limitation where bindings generated
 * from `@ContributesBinding` and `@ContributesTo` in `commonMain` source sets are not properly
 * visible to platform-specific source sets (like `appleMain`) during compilation.
 *
 * This is due to how KMP separates common and platform source folders during compilation.
 * See: https://kotlinlang.org/docs/whatsnew20.html#separation-of-common-and-platform-sources-during-compilation
 *
 * ## The Problem:
 * The `MockDeviceConnectionModule` in `components/bridge/transport/mock/impl/src/commonMain`
 * should provide the binding for `MockDeviceConnectionApi`, but when compiling iOS targets,
 * this binding is not visible, causing build failures:
 * ```
 * e: [ksp] Cannot find an @Inject constructor or provider for: ...
 * ```
 *
 * ## The Solution:
 * This module duplicates the same binding in `appleMain`, making it visible to iOS compilations.
 * This is a temporary workaround until kotlin-inject-anvil properly supports this use case
 * in multiplatform projects.
 *
 * ## Can this be removed?
 * Not yet. The workaround is necessary because:
 * 1. KSP processors run separately for each target
 * 2. Generated code from commonMain KSP is not accessible to platform targets
 * 3. Android works because it has its own compilation path that includes commonMain properly
 * 4. iOS requires explicit bindings in appleMain
 *
 * ## Related:
 * - MockDeviceConnectionModule: components/bridge/transport/mock/impl/.../MockDeviceConnectionModule.kt
 * - kotlin-inject multiplatform guide: https://github.com/evant/kotlin-inject/blob/main/docs/multiplatform.md
 * - kotlin-inject-anvil issues: https://github.com/amzn/kotlin-inject-anvil/issues
 */
@ContributesTo(BusyLibGraph::class)
interface WorkaroundDeviceConnectionModule {
    @IntoMap
    @Provides
    fun getWorkaroundMockDeviceConnection(
        mockDeviceConnectionApi: MockDeviceConnectionApi
    ): Pair<KClass<*>, DeviceConnectionApiHolder> {
        return FMockDeviceConnectionConfig::class to DeviceConnectionApiHolder(
            mockDeviceConnectionApi
        )
    }
}