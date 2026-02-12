plugins {
    id("flipper.multiplatform")
    id("flipper.anvil-multiplatform")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.components.core.wrapper)
        implementation(projects.components.core.ktx)
        implementation(projects.components.core.log)
        implementation(projects.components.core.di)

        implementation(projects.components.bridge.service.api)
        implementation(projects.components.bridge.orchestrator.api)
        implementation(projects.components.bridge.config.api)
        implementation(projects.components.watchers.api)

        implementation(libs.kotlin.coroutines)
    }
}
