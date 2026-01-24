plugins {
    id("flipper.multiplatform")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.components.bridge.orchestrator.api)
        implementation(projects.components.bridge.config.api)

        implementation(libs.kotlin.coroutines)
    }
}
