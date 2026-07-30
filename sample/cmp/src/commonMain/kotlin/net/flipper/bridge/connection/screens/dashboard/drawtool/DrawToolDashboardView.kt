package net.flipper.bridge.connection.screens.dashboard.drawtool

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.flipper.bridge.connection.feature.drawtool.api.model.DrawToolDisplaySide
import net.flipper.bridge.connection.screens.dashboard.common.DashboardActionState
import net.flipper.bridge.connection.screens.dashboard.common.DashboardButtonRow
import net.flipper.bridge.connection.screens.dashboard.common.DashboardLogCard
import net.flipper.bridge.connection.screens.dashboard.common.DashboardScreenLayout
import net.flipper.bridge.connection.screens.dashboard.common.DashboardSectionCard

@Composable
private fun DrawToolDisplaySection(
    onShowPreview: (DrawToolDisplaySide) -> Unit,
    onShowLatestStatus: (DrawToolDisplaySide) -> Unit,
    onHidePreview: () -> Unit,
    modifier: Modifier = Modifier
) {
    DashboardSectionCard(
        title = "Bar display",
        modifier = modifier
    ) {
        Text("Preview uploads the sample image and draws it. Status draws an uploaded bar file.")
        DashboardButtonRow(
            primaryTitle = "Preview → Front",
            onPrimaryClick = { onShowPreview(DrawToolDisplaySide.FRONT) },
            secondaryTitle = "Preview → Back",
            onSecondaryClick = { onShowPreview(DrawToolDisplaySide.BACK) }
        )
        DashboardButtonRow(
            primaryTitle = "Status → Front",
            onPrimaryClick = { onShowLatestStatus(DrawToolDisplaySide.FRONT) },
            secondaryTitle = "Status → Back",
            onSecondaryClick = { onShowLatestStatus(DrawToolDisplaySide.BACK) }
        )
        OutlinedButton(
            onClick = onHidePreview,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Hide")
        }
    }
}

@Composable
private fun DrawToolUploadSection(
    onUploadLatestStatus: () -> Unit,
    modifier: Modifier = Modifier
) {
    DashboardSectionCard(
        title = "Upload to bar",
        modifier = modifier
    ) {
        Text("Copies the latest client status into the collection of the bar. Draws nothing on its own.")
        OutlinedButton(
            onClick = onUploadLatestStatus,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Upload latest status")
        }
    }
}

@Composable
private fun DrawToolCollectionSection(
    target: DrawToolStorageTarget,
    onGenerateStatus: (DrawToolStorageTarget) -> Unit,
    onReadStatuses: (DrawToolStorageTarget) -> Unit,
    onDeleteStatuses: (DrawToolStorageTarget) -> Unit,
    modifier: Modifier = Modifier
) {
    DashboardSectionCard(
        title = "${target.title} collection",
        modifier = modifier
    ) {
        Text("Writes a UTC-named status PNG plus the shared preview, then reads the collection back into the log.")
        DashboardButtonRow(
            primaryTitle = "Generate status",
            onPrimaryClick = { onGenerateStatus(target) },
            secondaryTitle = "Read statuses",
            onSecondaryClick = { onReadStatuses(target) }
        )
        OutlinedButton(
            onClick = { onDeleteStatuses(target) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Delete statuses")
        }
    }
}

@Composable
fun DrawToolDashboardContent(
    onBack: () -> Unit,
    actionState: DashboardActionState,
    onShowPreview: (DrawToolDisplaySide) -> Unit,
    onShowLatestStatus: (DrawToolDisplaySide) -> Unit,
    onHidePreview: () -> Unit,
    onUploadLatestStatus: () -> Unit,
    onGenerateStatus: (DrawToolStorageTarget) -> Unit,
    onReadStatuses: (DrawToolStorageTarget) -> Unit,
    onDeleteStatuses: (DrawToolStorageTarget) -> Unit,
    modifier: Modifier = Modifier
) {
    DashboardScreenLayout(
        modifier = modifier,
        title = "Draw Tool",
        onBack = onBack
    ) {
        DrawToolDisplaySection(
            onShowPreview = onShowPreview,
            onShowLatestStatus = onShowLatestStatus,
            onHidePreview = onHidePreview,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        DrawToolUploadSection(
            onUploadLatestStatus = onUploadLatestStatus,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        DrawToolStorageTarget.entries.forEach { target ->
            DrawToolCollectionSection(
                target = target,
                onGenerateStatus = onGenerateStatus,
                onReadStatuses = onReadStatuses,
                onDeleteStatuses = onDeleteStatuses,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        DashboardLogCard(
            state = actionState,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}
