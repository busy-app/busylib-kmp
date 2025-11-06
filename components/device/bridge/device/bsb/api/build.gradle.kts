plugins {
    id("flipper.multiplatform")
    id("flipper.multiplatform-dependencies")
}

commonDependencies {
    implementation(projects.components.bsb.device.bridge.device.common.api)
    implementation(projects.components.bsb.device.bridge.feature.common.api)
    implementation(projects.components.bsb.device.bridge.transport.common.api)

    implementation(libs.kotlin.coroutines)
}
