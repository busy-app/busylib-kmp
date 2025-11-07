package com.flipperdevices.bsb.auth.principal.preference

import com.flipperdevices.bsb.auth.principal.api.BsbUserPrincipal
import com.flipperdevices.busylib.core.di.BusyLibGraph
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

interface BsbUserPrincipalKrate : FlowMutableKrate<BsbUserPrincipal>

@Inject
@ContributesBinding(BusyLibGraph::class, binding = binding<BsbUserPrincipalKrate>())
class BsbUserPrincipalKrateImpl(
    observableSettings: ObservableSettings,
    json: Json = Json {
        isLenient = true
        ignoreUnknownKeys = true
        prettyPrint = false
    }
) : BsbUserPrincipalKrate,
    FlowMutableKrate<BsbUserPrincipal> by DefaultFlowMutableKrate(
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
            if (newSettings == BsbUserPrincipal.Empty) {
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
        private const val KEY = "preference_bsb_user_principal"
        private val Factory get() = ValueFactory { BsbUserPrincipal.Loading }
        private val Serializer get() = BsbUserPrincipal.serializer()
    }
}
