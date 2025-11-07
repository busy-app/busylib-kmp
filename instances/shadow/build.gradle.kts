import com.android.build.gradle.tasks.BundleAar
import com.android.build.gradle.tasks.FusedLibraryBundle
import com.android.build.gradle.tasks.FusedLibraryBundleAar
import ru.astrainteractive.gradleplugin.property.baseGradleProperty
import ru.astrainteractive.gradleplugin.property.extension.AndroidModelPropertyValueExt.requireAndroidSdkInfo
import ru.astrainteractive.gradleplugin.property.extension.ModelPropertyValueExt.hierarchyGroup
import ru.astrainteractive.gradleplugin.property.extension.ModelPropertyValueExt.requireProjectInfo
import ru.astrainteractive.gradleplugin.property.extension.PrimitivePropertyValueExt.requireInt


plugins {
    id("dev.zacsweers.metro")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.android.fused-library")
}

dependencies {
    include(projects.components.di)
    include(projects.components.ktx)
    include(projects.components.log)
    include(projects.components.principal.api)
    include(projects.components.principal.impl)

    include(projects.components.bridge.config.api)
    include(projects.components.bridge.config.impl)
    include(projects.components.bridge.connectionbuilder.api)
    include(projects.components.bridge.connectionbuilder.impl)
    include(projects.components.bridge.device.bsb.api)
    include(projects.components.bridge.device.bsb.impl)
    include(projects.components.bridge.device.common.api)
    include(projects.components.bridge.device.firstpair.connection.api)
    include(projects.components.bridge.device.firstpair.connection.impl)
    include(projects.components.bridge.feature.battery.api)
    include(projects.components.bridge.feature.battery.impl)
    include(projects.components.bridge.feature.common.api)
    include(projects.components.bridge.feature.firmwareUpdate.api)
    include(projects.components.bridge.feature.firmwareUpdate.impl)
    include(projects.components.bridge.feature.info.api)
    include(projects.components.bridge.feature.info.impl)
    include(projects.components.bridge.feature.link.api)
    include(projects.components.bridge.feature.link.impl)
    include(projects.components.bridge.feature.provider.api)
    include(projects.components.bridge.feature.provider.impl)
    include(projects.components.bridge.feature.rpc.api)
    include(projects.components.bridge.feature.rpc.impl)
    include(projects.components.bridge.feature.screenStreaming.api)
    include(projects.components.bridge.feature.screenStreaming.impl)
    include(projects.components.bridge.feature.sync.impl)
    include(projects.components.bridge.feature.wifi.api)
    include(projects.components.bridge.feature.wifi.impl)
    include(projects.components.bridge.orchestrator.api)
    include(projects.components.bridge.orchestrator.impl)
    include(projects.components.bridge.service.api)
    include(projects.components.bridge.service.impl)

    include(projects.components.bridge.transport.ble.api)
    include(projects.components.bridge.transport.ble.impl)
    include(projects.components.bridge.transport.common.api)
    include(projects.components.bridge.transport.common.impl)
    include(projects.components.bridge.transport.mock.api)
    include(projects.components.bridge.transport.mock.impl)
    include(projects.components.bridge.transportconfigbuilder.api)
    include(projects.components.bridge.transportconfigbuilder.impl)

}

androidFusedLibrary {
    namespace = hierarchyGroup
    minSdk = requireAndroidSdkInfo.min

}

tasks.withType<FusedLibraryBundle> {
    archiveFileName = "${requireProjectInfo.name}-${requireProjectInfo.versionString}.aar"
}


