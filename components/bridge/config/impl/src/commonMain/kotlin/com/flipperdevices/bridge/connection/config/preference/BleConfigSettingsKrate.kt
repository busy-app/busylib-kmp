package com.flipperdevices.bridge.connection.config.preference

import com.flipperdevices.bridge.connection.config.impl.BleConfigSettings
import com.flipperdevices.core.di.AppGraph
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.coroutines.toFlowSettings
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import ru.astrainteractive.klibs.kstorage.api.value.ValueFactory
import ru.astrainteractive.klibs.kstorage.suspend.FlowMutableKrate
import ru.astrainteractive.klibs.kstorage.suspend.impl.DefaultFlowMutableKrate

interface BleConfigSettingsKrate : FlowMutableKrate<BleConfigSettings>

@Inject
@ContributesBinding(AppGraph::class, binding = binding<BleConfigSettingsKrate>())
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
                        json.decodeFromString(Serializer, stringValue)
                    }
                }
                .catch { null }
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
