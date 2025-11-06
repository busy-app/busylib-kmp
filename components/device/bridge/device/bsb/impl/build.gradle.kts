plugins {
    id("flipper.multiplatform")
    id("flipper.multiplatform-dependencies")
    id("flipper.anvil-multiplatform")
}

commonDependencies {
    implementation(projects.components.device.bridge.device.bsb.api)

    implementation(projects.components.di)
    implementation(projects.components.log)

    implementation(projects.components.device.bridge.device.common.api)
    implementation(projects.components.device.bridge.feature.common.api)
    implementation(projects.components.device.bridge.transport.common.api)

    implementation(projects.components.device.bridge.feature.rpc.api)
    implementation(projects.components.device.bridge.feature.info.api)
    implementation(projects.components.device.bridge.feature.battery.api)
    implementation(projects.components.device.bridge.feature.wifi.api)
    implementation(projects.components.device.bridge.feature.link.api)
    implementation(projects.components.device.bridge.feature.screenStreaming.api)
    implementation(projects.components.device.bridge.feature.firmwareUpdate.api)

    implementation(libs.kotlin.coroutines)
    implementation(libs.kotlin.immutable)
}
