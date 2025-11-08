plugins {
    id("flipper.multiplatform")
    id("ru.astrainteractive.gradleplugin.java.core")
    id("ru.astrainteractive.gradleplugin.android.namespace")
    id("ru.astrainteractive.gradleplugin.android.core")
    id("ru.astrainteractive.gradleplugin.publication")
}


kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.components.bridge.device.bsb.api)

        implementation(projects.components.log)
        implementation(projects.components.buildkonfig)

        implementation(projects.components.bridge.device.common.api)
        implementation(projects.components.bridge.feature.common.api)
        implementation(projects.components.bridge.transport.common.api)

        implementation(projects.components.bridge.feature.rpc.api)
        implementation(projects.components.bridge.feature.info.api)
        implementation(projects.components.bridge.feature.battery.api)
        implementation(projects.components.bridge.feature.wifi.api)
        implementation(projects.components.bridge.feature.link.api)
        implementation(projects.components.bridge.feature.screenStreaming.api)
        implementation(projects.components.bridge.feature.firmwareUpdate.api)

        implementation(libs.kotlin.coroutines)
        implementation(libs.kotlin.immutable)
    }
}
