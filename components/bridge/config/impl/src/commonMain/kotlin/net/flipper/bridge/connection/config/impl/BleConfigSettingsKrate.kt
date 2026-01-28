package net.flipper.bridge.connection.config.impl

import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.coroutines.toFlowSettings
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import ru.astrainteractive.klibs.kstorage.api.value.ValueFactory
import ru.astrainteractive.klibs.kstorage.suspend.FlowMutableKrate
import ru.astrainteractive.klibs.kstorage.suspend.impl.DefaultFlowMutableKrate

interface BleConfigSettingsKrate : FlowMutableKrate<BleConfigSettings>

class BleConfigSettingsKrateImpl(
    observableSettings: ObservableSettings,
    json: Json = Json {
        isLenient = true
        ignoreUnknownKeys = true
        prettyPrint = false
    }
) : BleConfigSettingsKrate,
    FlowMutableKrate<BleConfigSettings> by DefaultFlowMutableKrate(
        factory = { Factory.create() },
        loader = {
            observableSettings
                .toFlowSettings()
                .getStringOrNullFlow(KEY)
                .map { stringValue ->
                    if (stringValue.isNullOrBlank()) {
                        Factory.create()
                    } else {
                        runCatching { json.decodeFromString(Serializer, stringValue) }
                            .getOrNull()
                            ?: Factory.create()
                    }
                }
        },
        saver = { newSettings ->
            if (newSettings == null) {
                observableSettings.remove(KEY)
            } else {
                observableSettings.putString(
                    KEY,
                    json.encodeToString(Serializer, newSettings)
                )
            }
        }
    ) {
    companion object {
        private const val KEY = "ble_config"
        private val Factory get() = ValueFactory { BleConfigSettings() }
        private val Serializer get() = BleConfigSettings.serializer()
    }
}
