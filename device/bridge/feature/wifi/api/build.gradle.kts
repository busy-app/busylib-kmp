plugins {
    id("flipper.multiplatform")
    id("flipper.multiplatform-dependencies")
    id("kotlinx-serialization")
}
commonDependencies {
    implementation(projects.ktx)

    api(projects.device.bridge.feature.common.api)
    api(projects.device.bridge.feature.rpc.api)

    implementation(libs.kotlin.coroutines)
    implementation(libs.kotlin.immutable)
    implementation(libs.kotlin.serialization.json)
}
