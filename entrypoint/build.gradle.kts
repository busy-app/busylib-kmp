import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.konan.target.Family

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("ru.astrainteractive.gradleplugin.java.core")
    id("com.android.library")
    id("ru.astrainteractive.gradleplugin.android.namespace")
    id("ru.astrainteractive.gradleplugin.android.core")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("dev.zacsweers.metro")
}

kotlin {
    jvm()
    androidTarget()
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    macosX64()
    macosArm64()

    applyDefaultHierarchyTemplate()

    targets
        .filterIsInstance<KotlinNativeTarget>()
        .filter { it.konanTarget.family == Family.IOS || it.konanTarget.family == Family.OSX }
        .forEach {
            it.binaries.framework {
                baseName = "BridgeConnection"
                isStatic = true

                export(projects.components.di)
                export(projects.components.ktx)
                export(projects.components.log)
                export(projects.components.principal.api)
                export(projects.components.cloud.api)

                export(projects.components.bridge.config.api)
                export(projects.components.bridge.connectionbuilder.api)
                export(projects.components.bridge.connectionbuilder.impl)
                export(projects.components.bridge.device.bsb.api)
                export(projects.components.bridge.device.bsb.impl)
                export(projects.components.bridge.device.common.api)
                export(projects.components.bridge.feature.battery.api)
                export(projects.components.bridge.feature.battery.impl)
                export(projects.components.bridge.feature.common.api)
                export(projects.components.bridge.feature.firmwareUpdate.api)
                export(projects.components.bridge.feature.firmwareUpdate.impl)
                export(projects.components.bridge.feature.info.api)
                export(projects.components.bridge.feature.info.impl)
                export(projects.components.bridge.feature.link.api)
                export(projects.components.bridge.feature.link.impl)
                export(projects.components.bridge.feature.provider.api)
                export(projects.components.bridge.feature.provider.impl)
                export(projects.components.bridge.feature.rpc.api)
                export(projects.components.bridge.feature.rpc.impl)
                export(projects.components.bridge.feature.screenStreaming.api)
                export(projects.components.bridge.feature.screenStreaming.impl)
                export(projects.components.bridge.feature.sync.impl)
                export(projects.components.bridge.feature.wifi.api)
                export(projects.components.bridge.feature.wifi.impl)
                export(projects.components.bridge.orchestrator.api)
                export(projects.components.bridge.orchestrator.impl)
                export(projects.components.bridge.service.api)
                export(projects.components.bridge.service.impl)

                export(projects.components.bridge.transport.ble.api)
                export(projects.components.bridge.transport.common.api)
                export(projects.components.bridge.transport.common.impl)
                export(projects.components.bridge.transport.mock.api)
                export(projects.components.bridge.transport.mock.impl)
                export(projects.components.bridge.transportconfigbuilder.api)
                export(projects.components.bridge.transportconfigbuilder.impl)
                export(libs.kotlin.coroutines)
                export(projects.components.bridge.transport.ble.impl)
            }
        }
}

kotlin {
    sourceSets.commonMain.dependencies {
        api(projects.components.di)
        api(projects.components.ktx)
        api(projects.components.log)
        api(projects.components.principal.api)
        api(projects.components.cloud.api)

        api(projects.components.bridge.config.api)
        api(projects.components.bridge.connectionbuilder.api)
        api(projects.components.bridge.connectionbuilder.impl)
        api(projects.components.bridge.device.bsb.api)
        api(projects.components.bridge.device.bsb.impl)
        api(projects.components.bridge.device.common.api)
        api(projects.components.bridge.feature.battery.api)
        api(projects.components.bridge.feature.battery.impl)
        api(projects.components.bridge.feature.common.api)
        api(projects.components.bridge.feature.firmwareUpdate.api)
        api(projects.components.bridge.feature.firmwareUpdate.impl)
        api(projects.components.bridge.feature.info.api)
        api(projects.components.bridge.feature.info.impl)
        api(projects.components.bridge.feature.link.api)
        api(projects.components.bridge.feature.link.impl)
        api(projects.components.bridge.feature.provider.api)
        api(projects.components.bridge.feature.provider.impl)
        api(projects.components.bridge.feature.rpc.api)
        api(projects.components.bridge.feature.rpc.impl)
        api(projects.components.bridge.feature.screenStreaming.api)
        api(projects.components.bridge.feature.screenStreaming.impl)
        api(projects.components.bridge.feature.sync.impl)
        api(projects.components.bridge.feature.wifi.api)
        api(projects.components.bridge.feature.wifi.impl)
        api(projects.components.bridge.orchestrator.api)
        api(projects.components.bridge.orchestrator.impl)
        api(projects.components.bridge.service.api)
        api(projects.components.bridge.service.impl)

        api(projects.components.bridge.transport.ble.api)
        api(projects.components.bridge.transport.common.api)
        api(projects.components.bridge.transport.common.impl)
        api(projects.components.bridge.transport.mock.api)
        api(projects.components.bridge.transport.mock.impl)
        api(projects.components.bridge.transportconfigbuilder.api)
        api(projects.components.bridge.transportconfigbuilder.impl)
        api(libs.kotlin.coroutines)
    }
    sourceSets.androidMain.dependencies {
        api(projects.components.bridge.device.firstpair.connection.api)
        api(projects.components.bridge.device.firstpair.connection.impl)
        api(projects.components.bridge.transport.ble.impl)
    }
    sourceSets.appleMain.dependencies {
        api(projects.components.bridge.transport.ble.impl)
    }
}
