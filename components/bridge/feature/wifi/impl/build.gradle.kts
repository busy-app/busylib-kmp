plugins {
    id("flipper.multiplatform")
    id("flipper.anvil-multiplatform")
    id("ru.astrainteractive.gradleplugin.java.core")
    id("ru.astrainteractive.gradleplugin.android.namespace")
    id("ru.astrainteractive.gradleplugin.android.core")
    
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.components.bridge.feature.wifi.api)

        implementation(projects.components.core.di)
        implementation(projects.components.core.ktx)
        implementation(projects.components.core.log)

        implementation(projects.components.bridge.feature.common.api)
        implementation(projects.components.bridge.transport.common.api)

        implementation(projects.components.bridge.feature.rpc.api)

        implementation(libs.kotlin.coroutines)
        implementation(libs.kotlin.immutable)
    }

    sourceSets.commonTest.dependencies {
        implementation(libs.kotlin.test)
    }
}
