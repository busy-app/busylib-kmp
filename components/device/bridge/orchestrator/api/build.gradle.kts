plugins {
    id("flipper.multiplatform")
    id("flipper.multiplatform-dependencies")
}

commonDependencies {
    implementation(projects.components.bsb.device.bridge.transport.common.api)

    implementation(projects.components.bsb.device.bridge.config.api)

    implementation(libs.kotlin.coroutines)
}
