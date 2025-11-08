plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("ru.astrainteractive.gradleplugin.java.core")
    id("com.android.kotlin.multiplatform.library")
    id("ru.astrainteractive.gradleplugin.android.namespace")
    id("ru.astrainteractive.gradleplugin.android.core")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("dev.zacsweers.metro")
}

kotlin {
    jvm()
    androidLibrary {}
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    macosX64()
    macosArm64()

    applyDefaultHierarchyTemplate()
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.components.di)
        implementation(projects.components.ktx)
        implementation(projects.components.log)
        api(projects.components.principal.api)
        api(projects.components.cloud.api)

        api(projects.components.bridge.config.api)
        implementation(projects.components.bridge.connectionbuilder.api)
        implementation(projects.components.bridge.connectionbuilder.impl)
        implementation(projects.components.bridge.device.bsb.api)
        implementation(projects.components.bridge.device.bsb.impl)
        api(projects.components.bridge.device.common.api)
        api(projects.components.bridge.feature.battery.api)
        implementation(projects.components.bridge.feature.battery.impl)
        implementation(projects.components.bridge.feature.common.api)
        api(projects.components.bridge.feature.firmwareUpdate.api)
        implementation(projects.components.bridge.feature.firmwareUpdate.impl)
        api(projects.components.bridge.feature.info.api)
        implementation(projects.components.bridge.feature.info.impl)
        api(projects.components.bridge.feature.link.api)
        implementation(projects.components.bridge.feature.link.impl)
        api(projects.components.bridge.feature.provider.api)
        implementation(projects.components.bridge.feature.provider.impl)
        implementation(projects.components.bridge.feature.rpc.api)
        implementation(projects.components.bridge.feature.rpc.impl)
        api(projects.components.bridge.feature.screenStreaming.api)
        implementation(projects.components.bridge.feature.screenStreaming.impl)
        implementation(projects.components.bridge.feature.sync.impl)
        api(projects.components.bridge.feature.wifi.api)
        implementation(projects.components.bridge.feature.wifi.impl)
        api(projects.components.bridge.orchestrator.api)
        implementation(projects.components.bridge.orchestrator.impl)
        api(projects.components.bridge.service.api)
        implementation(projects.components.bridge.service.impl)

        implementation(projects.components.bridge.transport.ble.api)
        implementation(projects.components.bridge.transport.common.api)
        implementation(projects.components.bridge.transport.common.impl)
        implementation(projects.components.bridge.transport.mock.api)
        implementation(projects.components.bridge.transport.mock.impl)
        implementation(projects.components.bridge.transportconfigbuilder.api)
        implementation(projects.components.bridge.transportconfigbuilder.impl)
        implementation(libs.kotlin.coroutines)
    }
    sourceSets.androidMain.dependencies {
        api(projects.components.bridge.device.firstpair.connection.api)
        implementation(projects.components.bridge.device.firstpair.connection.impl)
        implementation(projects.components.bridge.transport.ble.impl)
    }
}
