plugins {
    id("flipper.multiplatform")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.components.bridge.feature.firmwareUpdate.api)
        implementation(projects.components.core.wrapper)

        implementation(libs.kotlin.coroutines)
    }
}
