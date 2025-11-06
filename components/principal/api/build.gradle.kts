plugins {
    kotlin("multiplatform")
    id("com.android.library")
    id("org.jetbrains.kotlin.plugin.serialization")
}

kotlin {
    jvm()
    androidTarget()
    applyDefaultHierarchyTemplate()
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(libs.kotlin.coroutines)
        implementation(libs.kotlin.serialization.json)
    }
}
