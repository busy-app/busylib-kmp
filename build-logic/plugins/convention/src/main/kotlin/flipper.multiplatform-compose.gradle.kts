plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    id("suppress-optin")
}

kotlin {
    jvm()
    androidLibrary {}

    applyDefaultHierarchyTemplate()
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(kotlin.compose.runtime)
        implementation(kotlin.compose.ui)
    }
}
