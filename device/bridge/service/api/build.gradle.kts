plugins {
    id("flipper.multiplatform")
    id("flipper.multiplatform-dependencies")
}

commonDependencies {
    implementation(projects.device.bridge.orchestrator.api)
    implementation(projects.device.bridge.config.api)

    implementation(libs.kotlin.coroutines)
}
