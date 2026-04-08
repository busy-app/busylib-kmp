plugins {
    id("flipper.multiplatform")
}

kotlin {
    sourceSets.commonMain.dependencies {
        api(projects.components.bridge.config.api)

        api(projects.components.bridge.feature.screenStreaming.api)

        implementation(libs.kotlin.coroutines)
    }
}
