plugins {
    id("flipper.multiplatform")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("ru.astrainteractive.gradleplugin.java.core")
    id("ru.astrainteractive.gradleplugin.android.namespace")
    id("ru.astrainteractive.gradleplugin.android.core")
    id("ru.astrainteractive.gradleplugin.publication")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.components.ktx)

        implementation(libs.kotlin.coroutines)
        implementation(libs.kotlin.serialization.json)
    }
}
