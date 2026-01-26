plugins {
    id("flipper.multiplatform")
    id("org.jetbrains.kotlin.plugin.serialization")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(libs.kotlin.coroutines)
        implementation(projects.components.principal.api)
        implementation(projects.components.core.wrapper)
    }
}
