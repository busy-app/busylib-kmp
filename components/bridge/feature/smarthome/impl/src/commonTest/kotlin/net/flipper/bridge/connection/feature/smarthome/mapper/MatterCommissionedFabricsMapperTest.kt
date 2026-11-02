@file:Suppress("FunctionNaming")

package net.flipper.bridge.connection.feature.smarthome.mapper

import net.flipper.bridge.connection.feature.rpc.api.model.MatterCommissionedFabrics
import net.flipper.bridge.connection.feature.rpc.api.model.MatterCommissionedFabrics.CommissioningStatus
import net.flipper.bridge.connection.feature.rpc.api.model.MatterCommissionedFabrics.CommissioningStatusType
import net.flipper.bridge.connection.feature.smarthome.model.BsbMatterCommissionedFabrics
import net.flipper.bridge.connection.feature.smarthome.model.BsbMatterCommissionedFabrics.BsbCommissioningStatus
import net.flipper.bridge.connection.feature.smarthome.model.BsbMatterCommissionedFabrics.BsbCommissioningStatusType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class MatterCommissionedFabricsMapperTest {

    @Test
    fun GIVEN_rpc_status_with_timestamp_WHEN_mapped_THEN_timestamp_is_kept() {
        val rpc = MatterCommissionedFabrics(
            fabricCount = 2,
            latestCommissioningStatus = CommissioningStatus(
                timestamp = PAIRED_AT,
                value = CommissioningStatusType.COMPLETED_SUCCESSFULLY
            )
        )

        assertEquals(
            BsbMatterCommissionedFabrics(
                fabricCount = 2,
                latestCommissioningStatus = BsbCommissioningStatus(
                    value = BsbCommissioningStatusType.COMPLETED_SUCCESSFULLY,
                    timestamp = PAIRED_AT
                )
            ),
            rpc.toBsbMatterCommissionedFabrics()
        )
    }

    @Test
    fun GIVEN_rpc_status_without_timestamp_WHEN_mapped_THEN_timestamp_is_null() {
        val rpc = MatterCommissionedFabrics(
            fabricCount = 0,
            latestCommissioningStatus = CommissioningStatus(
                timestamp = null,
                value = CommissioningStatusType.NEVER_STARTED
            )
        )

        assertEquals(
            BsbMatterCommissionedFabrics(
                fabricCount = 0,
                latestCommissioningStatus = BsbCommissioningStatus(
                    value = BsbCommissioningStatusType.NEVER_STARTED,
                    timestamp = null
                )
            ),
            rpc.toBsbMatterCommissionedFabrics()
        )
    }

    @Test
    fun GIVEN_bsb_status_with_timestamp_WHEN_mapped_back_THEN_timestamp_is_kept() {
        val bsb = BsbMatterCommissionedFabrics(
            fabricCount = 1,
            latestCommissioningStatus = BsbCommissioningStatus(
                value = BsbCommissioningStatusType.FAILED,
                timestamp = PAIRED_AT
            )
        )

        assertEquals(
            MatterCommissionedFabrics(
                fabricCount = 1,
                latestCommissioningStatus = CommissioningStatus(
                    timestamp = PAIRED_AT,
                    value = CommissioningStatusType.FAILED
                )
            ),
            bsb.toMatterCommissionedFabrics()
        )
    }

    @Test
    fun GIVEN_bsb_status_without_timestamp_WHEN_mapped_back_THEN_timestamp_is_null() {
        val bsb = BsbMatterCommissionedFabrics(
            fabricCount = 1,
            latestCommissioningStatus = BsbCommissioningStatus(value = BsbCommissioningStatusType.STARTED)
        )

        assertEquals(
            MatterCommissionedFabrics(
                fabricCount = 1,
                latestCommissioningStatus = CommissioningStatus(
                    timestamp = null,
                    value = CommissioningStatusType.STARTED
                )
            ),
            bsb.toMatterCommissionedFabrics()
        )
    }

    private companion object {
        val PAIRED_AT = Instant.fromEpochSeconds(1_793_633_560)
    }
}
