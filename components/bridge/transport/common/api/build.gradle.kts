plugins {
    id("flipper.multiplatform")
}

kotlin {
    sourceSets.commonMain.dependencies {
        api(projects.components.core.data)
        api(libs.kotlin.immutable)

        implementation(libs.kotlin.coroutines)
        implementation(libs.ktor.client.core)
    }
}
