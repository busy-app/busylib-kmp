plugins {
    id("flipper.multiplatform")
    id("flipper.anvil-multiplatform")
    id("ru.astrainteractive.gradleplugin.java.core")
    id("ru.astrainteractive.gradleplugin.android.namespace")
    id("ru.astrainteractive.gradleplugin.android.core")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.components.bridge.transport.common.api)
        implementation(projects.components.bridge.transport.combined.api)
        implementation(projects.components.core.di)

        implementation(libs.kotlin.coroutines)

        implementation(projects.components.bridge.connectionbuilder.api)
    }
}
