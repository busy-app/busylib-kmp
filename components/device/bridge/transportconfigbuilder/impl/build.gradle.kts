plugins {
    id("flipper.multiplatform")
    id("flipper.multiplatform-dependencies")
    id("flipper.anvil-multiplatform")
}

commonDependencies {
    implementation(projects.components.device.bridge.transportconfigbuilder.api)

    implementation(projects.components.di)

    implementation(projects.components.device.bridge.transport.common.api)
    implementation(projects.components.device.bridge.transport.mock.api)
    implementation(projects.components.device.bridge.transport.ble.api)
    implementation(projects.components.device.bridge.config.api)

    implementation(libs.kotlin.immutable)
}
