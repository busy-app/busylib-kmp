plugins {
    id("flipper.multiplatform")
    id("flipper.multiplatform-dependencies")
}
commonDependencies {
    implementation(projects.components.device.bridge.transport.common.api)
    implementation(libs.kotlin.coroutines)
    implementation(libs.kotlin.immutable)
}
