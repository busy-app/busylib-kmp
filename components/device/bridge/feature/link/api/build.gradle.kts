plugins {
    id("flipper.multiplatform")
    id("flipper.multiplatform-dependencies")
    id("kotlinx-serialization")
}
commonDependencies {
    implementation(projects.components.core.ktx)

    api(projects.components.bsb.device.bridge.feature.common.api)
    api(projects.components.bsb.device.bridge.feature.rpc.api)

    implementation(libs.kotlin.coroutines)
    implementation(libs.kotlin.immutable)
    implementation(libs.kotlin.serialization.json)
}
