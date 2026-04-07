plugins {
    id("flipper.multiplatform")
    id("kotlinx-serialization")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.components.core.log)
        implementation(libs.kotlin.serialization.json)
    }
}
