plugins {
    id("flipper.multiplatform")
}

kotlin {
    sourceSets.commonMain.dependencies {
        api(projects.components.core.data)
        implementation(libs.kotlin.coroutines)
    }
}
