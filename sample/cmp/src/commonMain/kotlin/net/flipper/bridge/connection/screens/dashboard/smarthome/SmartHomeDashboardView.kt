package net.flipper.bridge.connection.screens.dashboard.smarthome

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.flipper.bridge.connection.feature.rpc.api.model.MatterCommissionedFabrics
import net.flipper.bridge.connection.feature.smarthome.model.MatterCommissioningTimeLeftPayload
import net.flipper.bridge.connection.screens.dashboard.common.DashboardActionState
import net.flipper.bridge.connection.screens.dashboard.common.DashboardButtonRow
import net.flipper.bridge.connection.screens.dashboard.common.DashboardInfoRow
import net.flipper.bridge.connection.screens.dashboard.common.DashboardLogCard
import net.flipper.bridge.connection.screens.dashboard.common.DashboardScreenLayout
import net.flipper.bridge.connection.screens.dashboard.common.DashboardSectionCard
import net.flipper.bridge.connection.screens.dashboard.common.orUnavailable

@Composable
fun SmartHomeDashboardContent(
    onBack: () -> Unit,
    commissionedFabrics: MatterCommissionedFabrics?,
    pairCodeWithTimeLeft: MatterCommissioningTimeLeftPayload?,
    state: SmartHomeDashboardState,
    actionState: DashboardActionState,
    onRequestPairCode: () -> Unit,
    onForgetAllPairings: () -> Unit,
    modifier: Modifier = Modifier
) {
    DashboardScreenLayout(
        modifier = modifier,
        title = "Smart Home",
        onBack = onBack
    ) {
        DashboardSectionCard(
            title = "Pairing",
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            DashboardInfoRow(
                label = "Fabric count",
                value = commissionedFabrics?.fabricCount.orUnavailable()
            )
            DashboardInfoRow(
                label = "Pairing status",
                value = commissionedFabrics?.latestCommissioningStatus?.value.orUnavailable()
            )
            DashboardInfoRow(
                label = "Status timestamp",
                value = commissionedFabrics?.latestCommissioningStatus?.timestamp.orUnavailable()
            )
            DashboardInfoRow(
                label = "Time left",
                value = pairCodeWithTimeLeft?.timeLeft.orUnavailable()
            )
            Text("Last manual code: ${state.lastPairingCode.orUnavailable()}")
            Text("Last QR code: ${state.lastPairingQr.orUnavailable()}")
            Text("Available until: ${state.lastPairingExpiresAt.orUnavailable()}")
            DashboardButtonRow(
                primaryTitle = "Get Pair Code",
                onPrimaryClick = onRequestPairCode,
                secondaryTitle = "Forget Pairings",
                onSecondaryClick = onForgetAllPairings
            )
        }

        DashboardLogCard(
            state = actionState,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}
