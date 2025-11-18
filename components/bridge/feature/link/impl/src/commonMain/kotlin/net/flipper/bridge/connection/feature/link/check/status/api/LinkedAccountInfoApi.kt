package net.flipper.bridge.connection.feature.link.check.status.api

import kotlinx.coroutines.flow.MutableStateFlow
import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.feature.common.api.FFeatureInstanceKeeper
import net.flipper.bridge.connection.feature.link.model.LinkedAccountInfo

@Inject
class LinkedAccountInfoApi : FFeatureInstanceKeeper.Instance {
    val status = MutableStateFlow<LinkedAccountInfo?>(null)

    override fun dispose() = Unit
}
