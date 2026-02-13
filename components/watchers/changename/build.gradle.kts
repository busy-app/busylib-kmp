plugins {
    id("flipper.multiplatform")
    id("flipper.anvil-multiplatform")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.components.watchers.api)

        implementation(projects.components.core.di)
        implementation(projects.components.core.log)
        implementation(projects.components.core.wrapper)

        implementation(projects.components.bridge.config.api)
        implementation(projects.components.bridge.orchestrator.api)
        implementation(projects.components.bridge.feature.provider.api)
        implementation(projects.components.bridge.feature.settings.api)

        implementation(libs.kotlin.coroutines)
    }
}
