package net.flipper.bridge.connection.utils

import com.russhwolf.settings.Settings
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

val settingsJsonInstance = Json {
    isLenient = true
    ignoreUnknownKeys = true
    prettyPrint = false
}

inline fun <reified T : Any?> Settings.getSerializable(
    key: String,
    default: T
): T {
    val result = getStringOrNull(key)
    if (result.isNullOrBlank()) {
        return default
    }
    return settingsJsonInstance.decodeFromString(serializer<T>(), result)
}

inline fun <reified T : Any?> Settings.setSerializable(
    key: String,
    value: T
) {
    putString(key, settingsJsonInstance.encodeToString(serializer<T>(), value))
}
