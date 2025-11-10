plugins {
    id("flipper.multiplatform")
    id("ru.astrainteractive.gradleplugin.java.core")
    id("ru.astrainteractive.gradleplugin.android.namespace")
    id("ru.astrainteractive.gradleplugin.android.core")
    id("ru.astrainteractive.gradleplugin.publication")
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
