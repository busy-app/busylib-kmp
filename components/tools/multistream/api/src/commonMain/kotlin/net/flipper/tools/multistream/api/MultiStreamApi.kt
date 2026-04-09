package net.flipper.tools.multistream.api

import net.flipper.bridge.connection.config.api.model.BUSYBar
import net.flipper.busylib.core.wrapper.WrappedFlow

interface MultiStreamApi {
    fun get(busyBar: BUSYBar): WrappedFlow<MultiStreamState>
}
