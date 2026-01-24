plugins {
    id("flipper.multiplatform")
    id("flipper.anvil-multiplatform")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.components.core.di)

        implementation(projects.components.bridge.transport.common.api)

        implementation(libs.kotlin.coroutines)
    }
}
