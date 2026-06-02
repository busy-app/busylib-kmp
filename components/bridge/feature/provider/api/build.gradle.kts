plugins {
    id("flipper.multiplatform")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.components.bridge.feature.common.api)

        implementation(projects.components.bridge.orchestrator.api)

        implementation(libs.kotlin.coroutines)
    }
}
