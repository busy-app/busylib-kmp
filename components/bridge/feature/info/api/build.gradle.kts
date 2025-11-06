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
        implementation(projects.components.ktx)

        api(projects.components.bridge.feature.common.api)

        implementation(libs.kotlin.coroutines)
    }
}
