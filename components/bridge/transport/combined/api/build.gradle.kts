plugins {
    id("flipper.multiplatform")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.components.bridge.transport.common.api)
        implementation(libs.kotlin.coroutines)
        implementation(projects.components.bridge.connectionbuilder.api)
    }
}
