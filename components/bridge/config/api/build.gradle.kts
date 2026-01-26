plugins {
    id("flipper.multiplatform")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("kotlinx-serialization")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(libs.kotlin.coroutines)
        implementation(libs.kotlin.serialization.json)
    }
}
