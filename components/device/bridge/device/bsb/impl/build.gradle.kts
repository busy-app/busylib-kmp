plugins {
    id("flipper.multiplatform")
    id("flipper.multiplatform-dependencies")
    id("flipper.anvil-multiplatform")
}

commonDependencies {
    implementation(projects.components.bsb.device.bridge.device.bsb.api)

    implementation(projects.components.core.di)
    implementation(projects.components.core.log)
    implementation(projects.components.core.buildKonfig)

    implementation(projects.components.bsb.device.bridge.device.common.api)
    implementation(projects.components.bsb.device.bridge.feature.common.api)
    implementation(projects.components.bsb.device.bridge.transport.common.api)

    implementation(projects.components.bsb.device.bridge.feature.rpc.api)
    implementation(projects.components.bsb.device.bridge.feature.info.api)
    implementation(projects.components.bsb.device.bridge.feature.battery.api)
    implementation(projects.components.bsb.device.bridge.feature.wifi.api)
    implementation(projects.components.bsb.device.bridge.feature.link.api)
    implementation(projects.components.bsb.device.bridge.feature.screenStreaming.api)
    implementation(projects.components.bsb.device.bridge.feature.firmwareUpdate.api)

    implementation(libs.kotlin.coroutines)
    implementation(libs.kotlin.immutable)
}
