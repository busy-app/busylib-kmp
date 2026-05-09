package net.flipper.bridge.connection.feature.rpc.generated.api

import net.flipper.bridge.connection.feature.rpc.generated.model.NetworkInterfaceInfo
import net.flipper.bridge.connection.feature.rpc.generated.model.Status
import net.flipper.bridge.connection.feature.rpc.generated.model.StatusDevice
import net.flipper.bridge.connection.feature.rpc.generated.model.StatusFirmware
import net.flipper.bridge.connection.feature.rpc.generated.model.StatusPower
import net.flipper.bridge.connection.feature.rpc.generated.model.StatusSystem
import net.flipper.bridge.connection.feature.rpc.generated.model.VersionInfo

interface SystemApi {

    /**
     * Get device status
     */
    suspend fun getStatus(): kotlin.Result<Status>

    /**
     * Get device info
     */
    suspend fun getStatusDevice(): kotlin.Result<StatusDevice>

    /**
     * Get firmware info
     */
    suspend fun getStatusFirmware(): kotlin.Result<StatusFirmware>

    /**
     * Get power status
     */
    suspend fun getStatusPower(): kotlin.Result<StatusPower>

    /**
     * Get system status
     */
    suspend fun getStatusSystem(): kotlin.Result<StatusSystem>

    /**
     * Get device network connection info
     */
    suspend fun getTransport(): kotlin.Result<NetworkInterfaceInfo>

    /**
     * Get API version information
     */
    suspend fun getVersion(): kotlin.Result<VersionInfo>
}
