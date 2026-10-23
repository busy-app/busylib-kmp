@file:Suppress("FunctionNaming")

package net.flipper.bridge.connection.feature.settings.mapper

import net.flipper.bridge.connection.feature.rpc.api.exception.DeviceNameRejectedException
import net.flipper.bridge.connection.feature.settings.model.DeviceNameError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class DeviceNameErrorMapperTest {

    @Test
    fun GIVEN_disallowed_character_rejection_WHEN_mapped_THEN_is_DisallowedCharacter() {
        val error = DeviceNameRejectedException("Name contains disallowed character")

        assertIs<DeviceNameError.DisallowedCharacter>(error.toDeviceNameError())
    }

    @Test
    fun GIVEN_empty_name_rejection_WHEN_mapped_THEN_is_Empty() {
        val error = DeviceNameRejectedException("Name is empty")

        assertIs<DeviceNameError.Empty>(error.toDeviceNameError())
    }

    @Test
    fun GIVEN_too_long_rejection_WHEN_mapped_THEN_is_TooLong() {
        val error = DeviceNameRejectedException("Name is too long")

        assertIs<DeviceNameError.TooLong>(error.toDeviceNameError())
    }

    @Test
    fun GIVEN_undocumented_rejection_WHEN_mapped_THEN_is_left_alone() {
        val error = DeviceNameRejectedException("Storage is full")

        assertEquals(error, error.toDeviceNameError())
    }

    @Test
    fun GIVEN_transport_failure_WHEN_mapped_THEN_is_left_alone() {
        val error = IllegalStateException("Connection lost")

        assertEquals(error, error.toDeviceNameError())
    }
}
