plugins {
    id("flipper.multiplatform")
    id("flipper.multiplatform-dependencies")
    id("flipper.anvil-multiplatform")
}

commonDependencies {
    implementation(projects.ktx)
    implementation(projects.log)
    implementation(projects.di)

    implementation(projects.device.bridge.service.api)
    implementation(projects.device.bridge.orchestrator.api)
    implementation(projects.device.bridge.config.api)

    implementation(libs.kotlin.coroutines)
}
