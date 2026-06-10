plugins {
    id("flipper.multiplatform")
}

kotlin {
    sourceSets.commonMain.dependencies {
        api(projects.components.core.wrapper)
        api(projects.components.bridge.config.api)

        implementation(libs.kotlin.coroutines)
    }
}
