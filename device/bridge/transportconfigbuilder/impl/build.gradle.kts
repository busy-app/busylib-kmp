plugins {
    id("flipper.multiplatform")
    id("flipper.multiplatform-dependencies")
    id("flipper.anvil-multiplatform")
}

commonDependencies {
    implementation(projects.device.bridge.transportconfigbuilder.api)

    implementation(projects.di)

    implementation(projects.device.bridge.transport.common.api)
    implementation(projects.device.bridge.transport.mock.api)
    implementation(projects.device.bridge.transport.ble.api)
    implementation(projects.device.bridge.config.api)

    implementation(libs.kotlin.immutable)
}
