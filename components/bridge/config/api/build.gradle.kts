plugins {
    id("flipper.multiplatform")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("kotlinx-serialization")
}

kotlin {
    sourceSets.commonMain.dependencies {
        api(projects.components.core.wrapper)

        implementation(libs.kotlin.coroutines)
        implementation(libs.kotlin.serialization.json)
    }
}
