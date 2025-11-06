plugins {
    id("flipper.multiplatform")
    id("flipper.multiplatform-dependencies")
}

commonDependencies {
    implementation(projects.components.bsb.device.bridge.orchestrator.api)
    implementation(projects.components.bsb.device.bridge.config.api)

    implementation(libs.kotlin.coroutines)
}
