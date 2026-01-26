plugins {
    id("flipper.multiplatform")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.components.bridge.transport.common.api)

        implementation(projects.components.bridge.config.api)
        implementation(projects.components.core.wrapper)

        implementation(libs.kotlin.coroutines)
    }
}
