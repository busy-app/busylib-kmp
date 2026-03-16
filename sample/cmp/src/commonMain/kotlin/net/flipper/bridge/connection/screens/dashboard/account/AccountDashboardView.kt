package net.flipper.bridge.connection.screens.dashboard.account

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.flipper.bridge.connection.feature.link.model.LinkedAccountInfo
import net.flipper.bridge.connection.screens.dashboard.common.DashboardInfoRow
import net.flipper.bridge.connection.screens.dashboard.common.DashboardScreenLayout
import net.flipper.bridge.connection.screens.dashboard.common.DashboardSectionCard

@Composable
fun AccountDashboardContent(
    onBack: () -> Unit,
    linkedAccountStatus: LinkedAccountInfo?,
    onDeleteLinkedAccount: () -> Unit,
    modifier: Modifier = Modifier
) {
    DashboardScreenLayout(
        modifier = modifier,
        title = "Account",
        onBack = onBack
    ) {
        DashboardSectionCard(
            title = "Account",
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            DashboardInfoRow(label = "Linked account", value = linkedAccountStatus.toUiText())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onDeleteLinkedAccount,
                colors = ButtonDefaults.outlinedButtonColors(
                    backgroundColor = MaterialTheme.colors.surface
                )
            ) {
                Text("Delete linked account")
            }
        }
    }
}

private fun LinkedAccountInfo?.toUiText(): String {
    return when (this) {
        null -> "Unavailable"
        LinkedAccountInfo.NotLinked -> "Not linked"
        LinkedAccountInfo.Error -> "Error"
        LinkedAccountInfo.Disconnected -> "Disconnected"
        is LinkedAccountInfo.Linked.SameUser -> "Linked (same user: $linkedMail)"
        is LinkedAccountInfo.Linked.DifferentUser -> "Linked (different user: $linkedMail)"
        is LinkedAccountInfo.Linked.MissingBusyCloud -> "Linked (missing BusyCloud: $linkedMail)"
    }
}
