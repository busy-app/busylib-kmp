package net.flipper.bsb.cloud.barsws.api.utils

import io.ktor.serialization.WebsocketContentConverter
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.util.reflect.TypeInfo
import io.ktor.utils.io.charsets.Charset
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.serialization.json.Json
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.info

class LoggingWebsocketConverter(
    json: Json,
) : WebsocketContentConverter, LogTagProvider {
    override val TAG = "LoggingWebsocketConverter"
    private val delegate = KotlinxWebsocketSerializationConverter(json)

    override suspend fun serialize(
        charset: Charset,
        typeInfo: TypeInfo,
        value: Any?
    ): Frame {
        val frame = delegate.serialize(charset, typeInfo, value)
        info { ">>>>>>> $frame" }
        return frame
    }

    override suspend fun deserialize(
        charset: Charset,
        typeInfo: TypeInfo,
        content: Frame
    ): Any? {
        val text = when (content) {
            is Frame.Text -> content.readText()

            else -> content.toString()
        }
        info { "<<<<<<< $text" }
        if (delegate.isApplicable(content)) {
            val result = delegate.deserialize(charset, typeInfo, content)
            return result
        } else {
            return null
        }
    }

    override fun isApplicable(frame: Frame): Boolean {
        return true
    }
}
