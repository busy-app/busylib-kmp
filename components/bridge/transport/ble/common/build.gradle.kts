plugins {
    id("flipper.multiplatform")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.components.core.log)
        implementation(projects.components.core.ktx)

        implementation(libs.kotlin.coroutines)
        implementation(libs.ktor.client.core)
    }
}
