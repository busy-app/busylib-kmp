plugins {
    id("flipper.multiplatform")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.components.core.buildkonfig)
    }
    sourceSets.androidMain.dependencies {
        implementation(libs.timber)
    }
}
