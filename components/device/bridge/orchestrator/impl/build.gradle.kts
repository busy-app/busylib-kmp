plugins {
    id("flipper.multiplatform")
    id("flipper.multiplatform-dependencies")
    id("flipper.anvil-multiplatform")
}

commonDependencies {
    implementation(projects.components.core.ktx)
    implementation(projects.components.core.log)
    implementation(projects.components.core.di)

    implementation(projects.components.bsb.device.bridge.orchestrator.api)
    implementation(projects.components.bsb.device.bridge.transport.mock.api)
    implementation(projects.components.bsb.device.bridge.transport.common.api)
    implementation(projects.components.bsb.device.bridge.connectionbuilder.api)
    implementation(projects.components.bsb.device.bridge.config.api)
    implementation(projects.components.bsb.device.bridge.transportconfigbuilder.api)

    implementation(libs.kotlin.coroutines)
}
