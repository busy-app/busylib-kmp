plugins {
    id("flipper.multiplatform")
    id("kotlinx-serialization")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.components.core.wrapper)

        api(projects.components.bridge.feature.drawTool.api)

        implementation(libs.kotlin.coroutines)
        implementation(libs.kotlin.immutable)
        implementation(libs.kotlin.io)
        implementation(libs.kotlin.serialization.json)
    }
}
