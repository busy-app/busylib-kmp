plugins {
    id("flipper.multiplatform")
    id("flipper.anvil-multiplatform")
    id("org.jetbrains.kotlin.plugin.serialization")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.components.bridge.lanmonitor.api)

        implementation(projects.components.core.di)
        implementation(projects.components.core.log)
        implementation(projects.components.core.ktx)
        implementation(projects.components.core.ktor)
        implementation(projects.components.watchers.api)

        implementation(projects.components.bridge.config.api)
        implementation(projects.components.bridge.config.internal)

        implementation(libs.kotlin.coroutines)
        implementation(libs.ktor.client.core)
        implementation(libs.kotlin.serialization.json)

        implementation(projects.components.eventbus.api)
        implementation(projects.components.eventbus.internal)
    }

    sourceSets.commonTest.dependencies {
        implementation(libs.kotlin.test)
        implementation(libs.kotlin.coroutines.test)
    }
}
