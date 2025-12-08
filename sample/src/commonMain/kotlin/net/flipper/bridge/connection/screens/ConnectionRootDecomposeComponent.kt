package net.flipper.bridge.connection.screens

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.value.Value
import net.flipper.bridge.connection.screens.dashboard.DashboardDecomposeComponent
import net.flipper.bridge.connection.screens.decompose.CompositeDecomposeComponent
import net.flipper.bridge.connection.screens.decompose.DecomposeComponent
import net.flipper.bridge.connection.screens.device.ConnectionDeviceScreenDecomposeComponent
import net.flipper.bridge.connection.screens.models.ConnectionRootConfig
import net.flipper.bridge.connection.screens.nopermission.ConnectionNoPermissionDecomposeComponent
import net.flipper.bridge.connection.screens.search.ConnectionSearchDecomposeComponent
import net.flipper.bridge.connection.screens.utils.PermissionChecker

class ConnectionRootDecomposeComponent(
    componentContext: ComponentContext,
    permissionChecker: PermissionChecker,
    private val searchDecomposeFactory: ConnectionSearchDecomposeComponent.Factory,
    private val connectionDeviceScreenDecomposeComponentFactory: ConnectionDeviceScreenDecomposeComponent.Factory,
    private val dashboardDecomposeComponentFactory: DashboardDecomposeComponent.Factory
) : CompositeDecomposeComponent<ConnectionRootConfig>(), ComponentContext by componentContext {
    override val stack: Value<ChildStack<ConnectionRootConfig, DecomposeComponent>> = childStack(
        source = navigation,
        serializer = ConnectionRootConfig.serializer(),
        initialConfiguration = if (permissionChecker.isPermissionGranted()) {
            ConnectionRootConfig.Device
        } else {
            ConnectionRootConfig.NoPermission
        },
        childFactory = ::child,
        handleBackButton = true
    )

    private fun child(
        config: ConnectionRootConfig,
        componentContext: ComponentContext
    ): DecomposeComponent = when (config) {
        is ConnectionRootConfig.Search -> searchDecomposeFactory.invoke(
            componentContext = componentContext,
            onBack = navigation::pop
        )

        is ConnectionRootConfig.NoPermission ->
            ConnectionNoPermissionDecomposeComponent(componentContext)

        is ConnectionRootConfig.Device ->
            connectionDeviceScreenDecomposeComponentFactory(
                componentContext = componentContext,
                navigation = navigation
            )

        is ConnectionRootConfig.Dashboard -> dashboardDecomposeComponentFactory(
            componentContext = componentContext
        )
    }

    class Factory(
        private val permissionChecker: PermissionChecker,
        private val searchDecomposeFactory: ConnectionSearchDecomposeComponent.Factory,
        private val connectionDeviceScreenDecomposeComponentFactory: ConnectionDeviceScreenDecomposeComponent.Factory,
        private val dashboardDecomposeComponentFactory: DashboardDecomposeComponent.Factory
    ) {
        fun invoke(
            componentContext: ComponentContext
        ): ConnectionRootDecomposeComponent {
            return ConnectionRootDecomposeComponent(
                componentContext,
                permissionChecker,
                searchDecomposeFactory,
                connectionDeviceScreenDecomposeComponentFactory,
                dashboardDecomposeComponentFactory
            )
        }
    }
}
