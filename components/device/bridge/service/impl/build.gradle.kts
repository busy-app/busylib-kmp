plugins {
    id("flipper.multiplatform")
    id("flipper.multiplatform-dependencies")
    id("flipper.anvil-multiplatform")
}

commonDependencies {
    implementation(projects.components.core.ktx)
    implementation(projects.components.core.log)
    implementation(projects.components.core.di)

    implementation(projects.components.bsb.device.bridge.service.api)
    implementation(projects.components.bsb.device.bridge.orchestrator.api)
    implementation(projects.components.bsb.device.bridge.config.api)

    implementation(libs.kotlin.coroutines)
}
