plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.library")
    id("ru.astrainteractive.gradleplugin.android.namespace")
    id("ru.astrainteractive.gradleplugin.android.core")
}

kotlin {
    jvm()
    androidTarget()
    applyDefaultHierarchyTemplate()
}

kotlin {
    sourceSets.androidMain.dependencies {
        implementation(projects.components.log)
        implementation(projects.components.ktx)

        implementation(libs.kotlin.coroutines)
        implementation(libs.kotlin.immutable)
        implementation(libs.ble.client)

        implementation(libs.appcompat)
    }
}
