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
        implementation(projects.components.bridge.transport.common.api)
        implementation(libs.kotlin.coroutines)
    }
}
