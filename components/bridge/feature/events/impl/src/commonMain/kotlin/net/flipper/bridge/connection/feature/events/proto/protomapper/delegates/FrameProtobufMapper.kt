package net.flipper.bridge.connection.feature.events.proto.protomapper.delegates

import BSB_Frame.Encoding
import BSB_Frame.Frame
import BSB_Frame.PixelFormat
import BSB_Frame.Screen
import net.flipper.bridge.connection.feature.events.model.BusyLibUpdateEvent

object FrameProtobufMapper {
    fun map(frame: Frame): BusyLibUpdateEvent.Frame {
        return BusyLibUpdateEvent.Frame(
            screen = mapScreen(frame.screen),
            width = frame.width,
            height = frame.height,
            encoding = mapEncoding(frame.encoding),
            pixelFormat = mapPixelFormat(frame.pixel_format),
            data = frame.data_.toByteArray(),
        )
    }

    private fun mapScreen(screen: Screen): BusyLibUpdateEvent.Frame.Screen {
        return when (screen) {
            Screen.FRONT -> BusyLibUpdateEvent.Frame.Screen.FRONT
            Screen.BACK -> BusyLibUpdateEvent.Frame.Screen.BACK
            is Screen.Unrecognized -> BusyLibUpdateEvent.Frame.Screen.FRONT
        }
    }

    private fun mapEncoding(encoding: Encoding): BusyLibUpdateEvent.Frame.Encoding {
        return when (encoding) {
            Encoding.PLAIN -> BusyLibUpdateEvent.Frame.Encoding.PLAIN
            Encoding.RUN_LENGTH -> BusyLibUpdateEvent.Frame.Encoding.RUN_LENGTH
            Encoding.DEFLATE -> BusyLibUpdateEvent.Frame.Encoding.DEFLATE
            Encoding.DEFLATE_RUN_LENGTH -> BusyLibUpdateEvent.Frame.Encoding.DEFLATE_RUN_LENGTH
            is Encoding.Unrecognized -> BusyLibUpdateEvent.Frame.Encoding.PLAIN
        }
    }

    private fun mapPixelFormat(pixelFormat: PixelFormat): BusyLibUpdateEvent.Frame.PixelFormat {
        return when (pixelFormat) {
            PixelFormat.RGB888 -> BusyLibUpdateEvent.Frame.PixelFormat.RGB888
            PixelFormat.L8 -> BusyLibUpdateEvent.Frame.PixelFormat.L8
            PixelFormat.L4 -> BusyLibUpdateEvent.Frame.PixelFormat.L4
            is PixelFormat.Unrecognized -> BusyLibUpdateEvent.Frame.PixelFormat.RGB888
        }
    }
}
