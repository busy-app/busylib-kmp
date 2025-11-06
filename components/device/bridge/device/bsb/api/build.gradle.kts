plugins {
    id("flipper.multiplatform")
    id("flipper.multiplatform-dependencies")
}

commonDependencies {
    implementation(projects.components.device.bridge.device.common.api)
    implementation(projects.components.device.bridge.feature.common.api)
    implementation(projects.components.device.bridge.transport.common.api)

    implementation(libs.kotlin.coroutines)
}
