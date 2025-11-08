plugins {
    id("flipper.multiplatform-compose")
    id("ru.astrainteractive.gradleplugin.java.core")
    id("ru.astrainteractive.gradleplugin.android.namespace")
    id("ru.astrainteractive.gradleplugin.android.core")
    id("ru.astrainteractive.gradleplugin.publication")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.components.bridge.feature.screenStreaming.api)

        implementation(projects.components.ktx)
        implementation(projects.components.log)

        implementation(libs.kotlin.coroutines)

        implementation(kotlin.compose.runtime)
        implementation(kotlin.compose.ui)
    }
}
