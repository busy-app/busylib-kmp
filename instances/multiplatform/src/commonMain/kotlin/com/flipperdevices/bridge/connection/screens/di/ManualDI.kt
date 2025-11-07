package com.flipperdevices.bridge.connection.screens.di

import com.arkivanov.decompose.ComponentContext
import com.flipperdevices.bridge.connection.screens.ConnectionRootDecomposeComponent

fun getRootDecomposeComponent(
    componentContext: ComponentContext
): ConnectionRootDecomposeComponent {
    return getRootDecomposeComponentFactory().invoke(componentContext)
}

private fun getRootDecomposeComponentFactory(): ConnectionRootDecomposeComponent.Factory {
    TODO()
}