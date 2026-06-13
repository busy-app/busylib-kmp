plugins {
    id("flipper.multiplatform")
    id("flipper.metro-multiplatform")
    id("org.jetbrains.kotlin.plugin.serialization")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.components.cloud.rest.api)

        implementation(projects.components.core.di)
        implementation(projects.components.core.ktx)
        implementation(projects.components.core.ktor)
        implementation(projects.components.core.buildkonfig)
        implementation(projects.components.core.wrapper)

        implementation(projects.components.principal.api)
        implementation(projects.components.cloud.api)

        implementation(libs.ktor.client.core)
        implementation(libs.ktor.negotiation)
        implementation(libs.ktor.serialization)

        implementation(libs.kotlin.coroutines)
        implementation(libs.kotlin.serialization.json)
        implementation(libs.settings)
        implementation(libs.klibs.kstorage)
    }

    sourceSets.commonTest.dependencies {
        implementation(libs.kotlin.test)
        implementation(libs.kotlin.coroutines.test)
        implementation(libs.ktor.client.mock)
    }
}
