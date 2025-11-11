package net.flipper.bridge.connection.feature.screenstreaming.composable

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import net.flipper.bridge.connection.feature.screenstreaming.model.BusyImageFormat

private const val BUSY_BAR_SCREEN_WIDTH = 72
private const val BUSY_BAR_SCREEN_HEIGHT = 16
private const val DEFAULT_SCALE = 10
private const val MAX_COLOR_VALUE = 255f
private const val BYTE_COLOR_FLAG = 0xFF

@Suppress("MagicNumber")
private fun toImageBitmap(byteArray: ByteArray): ImageBitmap {
    val img = ImageBitmap(
        width = BUSY_BAR_SCREEN_WIDTH * DEFAULT_SCALE,
        height = BUSY_BAR_SCREEN_HEIGHT * DEFAULT_SCALE,
        config = ImageBitmapConfig.Argb8888
    )
    val canvas = Canvas(img)

    var i = 0
    for (y in 0 until BUSY_BAR_SCREEN_HEIGHT) {
        for (x in 0 until BUSY_BAR_SCREEN_WIDTH) {
            val blue = byteArray[i].toInt() and BYTE_COLOR_FLAG
            val green = byteArray[i + 1].toInt() and BYTE_COLOR_FLAG
            val red = byteArray[i + 2].toInt() and BYTE_COLOR_FLAG
            i += 3

            val paint = Paint()

            paint.color = Color(
                red = red / MAX_COLOR_VALUE,
                green = green / MAX_COLOR_VALUE,
                blue = blue / MAX_COLOR_VALUE
            )
            val sx = (x * DEFAULT_SCALE).toFloat()
            val sy = (y * DEFAULT_SCALE).toFloat()
            val rect = Rect(
                left = sx,
                top = sy,
                right = sx + DEFAULT_SCALE,
                bottom = sy + DEFAULT_SCALE
            )
            canvas.drawRect(rect, paint)
        }
    }
    return img
}

@Composable
fun rememberBusyImagePainter(image: BusyImageFormat?): Painter? {
    val density = LocalDensity.current
    val painter = remember { mutableStateOf<Painter?>(null) }
    LaunchedEffect(image, density) {
        val byteArray = image?.byteArray ?: return@LaunchedEffect
        withContext(Dispatchers.IO) {
            painter.value = runCatching {
                val bitmap = toImageBitmap(byteArray)
                BitmapPainter(bitmap)
            }.getOrNull()
        }
    }
    return painter.value
}
