plugins {
    id("flipper.multiplatform")
    id("flipper.multiplatform-dependencies")
    id("flipper.anvil-multiplatform")
}

commonDependencies {
    implementation(projects.components.core.di)

    implementation(projects.components.bsb.device.bridge.transport.common.api)

    implementation(libs.kotlin.coroutines)
}
