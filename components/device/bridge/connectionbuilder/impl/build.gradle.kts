plugins {
    id("flipper.multiplatform")
    id("flipper.multiplatform-dependencies")
    id("flipper.anvil-multiplatform")
}

commonDependencies {
    implementation(projects.components.bsb.device.bridge.connectionbuilder.api)

    implementation(projects.components.core.di)

    implementation(projects.components.bsb.device.bridge.transport.common.api)
    implementation(projects.components.bsb.device.bridge.transport.mock.api)

    implementation(libs.kotlin.coroutines)
}
