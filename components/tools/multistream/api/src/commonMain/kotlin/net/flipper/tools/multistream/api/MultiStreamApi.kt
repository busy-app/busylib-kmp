package net.flipper.tools.multistream.api

import kotlinx.coroutines.flow.Flow
import net.flipper.bridge.connection.config.api.model.BUSYBar


interface MultiStreamApi {
    fun get(busyBar: BUSYBar): Flow<MultiStreamState>
}