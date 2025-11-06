package com.flipperdevices.bridge.connection.screens.decompose

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.Lifecycle

abstract class ScreenDecomposeComponent(
    componentContext: ComponentContext
) : DecomposeComponent(),
    ComponentContext by componentContext,
    Lifecycle.Callbacks {
    init {
        lifecycle.subscribe(this)
    }

    override fun onResume() {
        super.onResume()
    }
}
