plugins {
    id("flipper.multiplatform")
    id("flipper.multiplatform-dependencies")
    id("kotlinx-serialization")
}
commonDependencies {
    implementation(projects.components.ktx)

    api(projects.components.device.bridge.feature.common.api)
    implementation(projects.components.device.bridge.transport.common.api)

    implementation(libs.kotlin.coroutines)
    implementation(libs.kotlin.serialization.json)

    implementation(libs.ktor.client.core)
}
