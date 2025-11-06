plugins {
    kotlin("multiplatform")
    id("com.android.library")
}

kotlin {
    jvm()
    androidTarget()
    applyDefaultHierarchyTemplate()
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.components.buildkonfig)
    }
    sourceSets.androidMain.dependencies {
        implementation(libs.timber)
    }
}
