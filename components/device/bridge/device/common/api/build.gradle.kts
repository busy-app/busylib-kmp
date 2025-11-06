plugins {
    id("flipper.multiplatform")
    id("flipper.multiplatform-dependencies")
}

commonDependencies {
    implementation(projects.components.device.bridge.feature.common.api)
    implementation(libs.kotlin.coroutines)
}
