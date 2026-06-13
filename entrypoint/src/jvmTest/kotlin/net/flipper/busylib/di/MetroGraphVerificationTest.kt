package net.flipper.busylib.di

import com.flipperdevices.core.network.BUSYLibNetworkStateApiNoop
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.Settings
import dev.zacsweers.metro.createGraphFactory
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import net.flipper.bridge.connection.feature.common.api.FDeviceFeature
import net.flipper.bsb.auth.principal.api.BUSYLibPrincipalApi
import net.flipper.bsb.auth.principal.api.BUSYLibUserPrincipal
import net.flipper.bsb.cloud.api.BUSYLibHostApiStub
import net.flipper.busylib.core.wrapper.WrappedStateFlow
import net.flipper.busylib.core.wrapper.wrap
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Runtime verification of the Metro DI graph against compiled binaries: aggregation completeness for
 * the feature/listener/connection multibindings, @SingleIn caching, and assisted-factory wiring.
 */
class MetroGraphVerificationTest {

    private val scope = CoroutineScope(Job())

    private val graph: VerificationGraph = createGraphFactory<VerificationGraph.Factory>().create(
        scope = scope,
        principalApi = FakePrincipalApi,
        observableSettings = mockk<ObservableSettings>(relaxed = true),
        hostApi = BUSYLibHostApiStub("cloud.busy.app"),
        networkStateApi = BUSYLibNetworkStateApiNoop(),
        settings = mockk<Settings>(relaxed = true)
    )

    @AfterTest
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun `feature factory map contains exactly one entry per FDeviceFeature`() {
        assertEquals(
            FDeviceFeature.entries.toSet(),
            graph.featureFactories.keys,
            "Every FDeviceFeature must have exactly one contributed FDeviceFeatureApi.Factory"
        )
    }

    @Test
    fun `startup listener set is populated`() {
        val listeners = graph.startupListeners
        assertTrue(listeners.isNotEmpty(), "InternalBUSYLibStartupListener multibinding must not be empty")
        assertEquals(
            listeners.size,
            listeners.distinct().size,
            "Each startup listener must be a distinct instance"
        )
    }

    @Test
    fun `device connection holder map is populated`() {
        assertTrue(
            graph.connectionHolders.isNotEmpty(),
            "Map<KClass, DeviceConnectionApiHolder> multibinding must not be empty"
        )
    }

    @Test
    fun `assisted bsb device factory is wired into the graph`() {
        assertNotNull(graph.bsbDeviceFactory, "Assisted FBSBDeviceApi.Factory binding must resolve")
    }

    @Test
    fun `SingleIn binding returns the same cached instance`() {
        assertSame(
            graph.eventBus,
            graph.eventBusProvider(),
            "A @SingleIn(BusyLibGraph) binding must resolve to one cached instance per graph"
        )
    }

    private object FakePrincipalApi : BUSYLibPrincipalApi {
        override fun getPrincipalFlow(): WrappedStateFlow<BUSYLibUserPrincipal> =
            MutableStateFlow<BUSYLibUserPrincipal>(BUSYLibUserPrincipal.Empty).wrap()
    }
}
