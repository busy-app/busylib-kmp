plugins {
    id("flipper.multiplatform")
    id("flipper.multiplatform-dependencies")
}

commonDependencies {
    implementation(projects.device.bridge.transport.common.api)
    implementation(projects.device.bridge.config.api)
}
