plugins {
    id("flipper.multiplatform")
    id("flipper.multiplatform-dependencies")
    id("flipper.anvil-multiplatform")
}

commonDependencies {
    implementation(projects.components.ktx)
    implementation(projects.components.log)
    implementation(projects.components.di)

    implementation(projects.components.device.bridge.orchestrator.api)
    implementation(projects.components.device.bridge.transport.mock.api)
    implementation(projects.components.device.bridge.transport.common.api)
    implementation(projects.components.device.bridge.connectionbuilder.api)
    implementation(projects.components.device.bridge.config.api)
    implementation(projects.components.device.bridge.transportconfigbuilder.api)

    implementation(libs.kotlin.coroutines)
}
