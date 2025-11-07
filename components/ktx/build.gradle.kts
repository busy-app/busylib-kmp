plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.library")
    id("ru.astrainteractive.gradleplugin.android.namespace")
    id("ru.astrainteractive.gradleplugin.android.core")
}

kotlin {
    jvm()
    androidTarget()
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    macosX64()
    macosArm64()

    applyDefaultHierarchyTemplate()
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.components.log)
        implementation(projects.components.buildkonfig)

        implementation(libs.kotlin.coroutines)
        implementation(libs.kotlin.datetime)

        implementation(libs.kotlin.io)
        implementation(libs.encoding.hash.md)
    }

    sourceSets.androidMain.dependencies {
        implementation(libs.androidx.core.ktx)
    }

    sourceSets.commonTest.dependencies {
        implementation(libs.kotlin.test)
        implementation(libs.kotlin.coroutines.test)
    }
}
