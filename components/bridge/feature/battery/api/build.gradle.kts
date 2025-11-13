plugins {
    id("flipper.multiplatform")
    id("ru.astrainteractive.gradleplugin.java.core")
    id("ru.astrainteractive.gradleplugin.android.namespace")
    id("ru.astrainteractive.gradleplugin.android.core")
    
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.components.core.ktx)

        api(projects.components.bridge.feature.common.api)

        implementation(libs.kotlin.coroutines)
        implementation(libs.kotlin.serialization.json)
    }
}
