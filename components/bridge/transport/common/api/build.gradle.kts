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
        implementation(libs.kotlin.coroutines)
        implementation(libs.ktor.client.core)
    }

}