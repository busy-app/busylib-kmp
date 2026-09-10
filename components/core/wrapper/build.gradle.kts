plugins {
    id("flipper.multiplatform")
}

kotlin {
    sourceSets.commonMain.dependencies {
        api(projects.components.core.data)
        implementation(projects.components.core.ktx)
        implementation(libs.kotlin.coroutines)
    }
}
