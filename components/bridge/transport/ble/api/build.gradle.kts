plugins {
    id("flipper.multiplatform")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.components.bridge.transport.common.api)
        implementation(projects.components.bridge.transport.ble.common)

        implementation(libs.kotlin.coroutines)
        implementation(libs.kotlin.immutable)
    }
}
