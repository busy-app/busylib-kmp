plugins {
    id("flipper.multiplatform")
    id("flipper.multiplatform-dependencies")
}

commonDependencies {
    implementation(projects.device.bridge.device.common.api)
    implementation(projects.device.bridge.feature.common.api)
    implementation(projects.device.bridge.transport.common.api)

    implementation(libs.kotlin.coroutines)
}
