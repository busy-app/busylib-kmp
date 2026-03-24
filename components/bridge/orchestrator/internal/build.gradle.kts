plugins {
    id("flipper.multiplatform")
}

kotlin {
    sourceSets.commonMain.dependencies {
        api(projects.components.bridge.orchestrator.api)

        implementation(projects.components.bridge.config.api)

        implementation(libs.kotlin.coroutines)
    }
}
