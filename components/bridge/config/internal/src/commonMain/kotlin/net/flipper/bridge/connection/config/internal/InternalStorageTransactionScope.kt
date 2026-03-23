package net.flipper.bridge.connection.config.internal

import net.flipper.bridge.connection.config.api.PersistedStorageTransactionScope
import net.flipper.bridge.connection.config.api.model.BUSYBar

/**
 * This is internal BUSY Lib class, don't use it outside BUSY Lib
 */
interface InternalStorageTransactionScope : PersistedStorageTransactionScope {
    fun setCurrentDeviceNullable(device: BUSYBar?)
    fun removeDevice(id: String)
}
