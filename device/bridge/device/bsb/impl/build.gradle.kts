plugins {
    id("flipper.multiplatform")
    id("flipper.multiplatform-dependencies")
    id("flipper.anvil-multiplatform")
}

commonDependencies {
    implementation(projects.device.bridge.device.bsb.api)

    implementation(projects.di)
    implementation(projects.log)

    implementation(projects.device.bridge.device.common.api)
    implementation(projects.device.bridge.feature.common.api)
    implementation(projects.device.bridge.transport.common.api)

    implementation(projects.device.bridge.feature.rpc.api)
    implementation(projects.device.bridge.feature.info.api)
    implementation(projects.device.bridge.feature.battery.api)
    implementation(projects.device.bridge.feature.wifi.api)
    implementation(projects.device.bridge.feature.link.api)
    implementation(projects.device.bridge.feature.screenStreaming.api)
    implementation(projects.device.bridge.feature.firmwareUpdate.api)

    implementation(libs.kotlin.coroutines)
    implementation(libs.kotlin.immutable)
}
