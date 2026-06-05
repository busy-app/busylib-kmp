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

        implementation(libs.kotlin.coroutines)
        implementation(libs.ktor.client.core)
    }

    sourceSets.jvmTest.dependencies {
        implementation(libs.kotlin.test)
        implementation(libs.kotlin.coroutines.test)
    }

    sourceSets.appleTest.dependencies {
        implementation(libs.kotlin.test)
        implementation(libs.kotlin.coroutines.test)
    }
}
