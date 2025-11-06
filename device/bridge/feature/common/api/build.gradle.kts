plugins {
    id("flipper.multiplatform")
    id("flipper.multiplatform-dependencies")
    id("flipper.anvil-multiplatform")
}

commonDependencies {
    implementation(projects.di)

    implementation(projects.device.bridge.transport.common.api)

    implementation(libs.kotlin.coroutines)
}
