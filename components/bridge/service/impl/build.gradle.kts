plugins {
    kotlin("multiplatform")
    id("com.android.library")
    id("dev.zacsweers.metro")
}

kotlin {
    jvm()
    androidTarget()
    applyDefaultHierarchyTemplate()
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.components.ktx)
        implementation(projects.components.log)
        implementation(projects.components.di)

        implementation(projects.components.bridge.service.api)
        implementation(projects.components.bridge.orchestrator.api)
        implementation(projects.components.bridge.config.api)

        implementation(libs.kotlin.coroutines)
    }
}
