package com.flipperdevices.bridge.connection.screens

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.value.Value
import com.flipperdevices.bridge.connection.screens.decompose.CompositeDecomposeComponent
import com.flipperdevices.bridge.connection.screens.decompose.DecomposeComponent
import com.flipperdevices.bridge.connection.screens.device.ConnectionDeviceScreenDecomposeComponent
import com.flipperdevices.bridge.connection.screens.models.ConnectionRootConfig
import com.flipperdevices.bridge.connection.screens.nopermission.ConnectionNoPermissionDecomposeComponent
import com.flipperdevices.bridge.connection.screens.search.ConnectionSearchDecomposeComponent
import com.flipperdevices.bridge.connection.screens.utils.PermissionChecker
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.Inject

@Inject
class ConnectionRootDecomposeComponent(
    @Assisted componentContext: ComponentContext,
    permissionChecker: PermissionChecker,
    private val searchDecomposeFactory: ConnectionSearchDecomposeComponent.Factory,
    private val connectionDeviceScreenDecomposeComponentFactory: ConnectionDeviceScreenDecomposeComponent.Factory
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
        is ConnectionRootConfig.Search -> searchDecomposeFactory(
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

        ConnectionRootConfig.FileManager -> TODO()
    }

    @AssistedFactory
    fun interface Factory {
        operator fun invoke(
            componentContext: ComponentContext
        ): ConnectionRootDecomposeComponent
    }
}
