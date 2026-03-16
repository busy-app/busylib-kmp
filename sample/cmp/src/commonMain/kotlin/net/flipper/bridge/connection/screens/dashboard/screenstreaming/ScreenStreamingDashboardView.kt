package net.flipper.bridge.connection.screens.dashboard.screenstreaming

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import net.flipper.bridge.connection.feature.screenstreaming.model.BusyImageFormat
import net.flipper.bridge.connection.screens.dashboard.common.DashboardScreenLayout
import net.flipper.bridge.connection.screens.dashboard.common.DashboardSectionCard
import net.flipper.bridge.connection.screens.dashboard.rememberBusyImagePainter

@Composable
fun ScreenStreamingDashboardContent(
    onBack: () -> Unit,
    streamImage: BusyImageFormat?,
    modifier: Modifier = Modifier
) {
    DashboardScreenLayout(
        modifier = modifier,
        title = "Screen Streaming",
        onBack = onBack
    ) {
        DashboardSectionCard(
            title = "Screen Streaming",
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            ScreenStreamingBlock(
                modifier = Modifier.fillMaxWidth(),
                image = streamImage
            )
        }
    }
}

@Composable
private fun ScreenStreamingBlock(
    image: BusyImageFormat?,
    modifier: Modifier = Modifier
) {
    val painter = rememberBusyImagePainter(image)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
    ) {
        if (painter != null) {
            Image(
                modifier = Modifier.fillMaxSize(),
                painter = painter,
                contentDescription = null,
                contentScale = ContentScale.FillWidth
            )
        } else {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.05f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "Waiting for stream...",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}
