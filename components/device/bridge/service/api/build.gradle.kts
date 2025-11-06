plugins {
    id("flipper.multiplatform")
    id("flipper.multiplatform-dependencies")
}

commonDependencies {
    implementation(projects.components.device.bridge.orchestrator.api)
    implementation(projects.components.device.bridge.config.api)

    implementation(libs.kotlin.coroutines)
}
