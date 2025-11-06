plugins {
    id("flipper.multiplatform")
    id("flipper.multiplatform-dependencies")
    id("flipper.anvil-multiplatform")
}

commonDependencies {
    implementation(projects.components.di)

    implementation(projects.components.device.bridge.transport.common.api)

    implementation(libs.kotlin.coroutines)
}
