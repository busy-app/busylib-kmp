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
        api(projects.components.bridge.feature.rpc.api)

        implementation(libs.kotlin.coroutines)
        implementation(libs.kotlin.immutable)
        implementation(libs.kotlin.serialization.json)
    }
}
