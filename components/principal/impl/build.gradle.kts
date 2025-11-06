plugins {
    kotlin("multiplatform")
    id("com.android.library")
    id("dev.zacsweers.metro")
    id("org.jetbrains.kotlin.plugin.serialization")
}

kotlin {
    jvm()
    androidTarget()
    applyDefaultHierarchyTemplate()
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.components.principal.api)

        implementation(projects.components.di)
        implementation(projects.components.log)

        implementation(libs.kotlin.coroutines)
        implementation(libs.kotlin.serialization.json)
        implementation(libs.klibs.kstorage)
        implementation(libs.settings)
        implementation(libs.settings.observable)
        implementation(libs.settings.coroutines)
    }
}
