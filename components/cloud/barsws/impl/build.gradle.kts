plugins {
    id("flipper.multiplatform")
    id("flipper.anvil-multiplatform")
    id("org.jetbrains.kotlin.plugin.serialization")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.components.cloud.barsws.api)

        implementation(projects.components.core.log)
        implementation(projects.components.core.di)
        implementation(projects.components.core.ktx)
        implementation(projects.components.core.ktor)
        implementation(projects.components.core.network)
        implementation(projects.components.core.wrapper)


        implementation(projects.components.principal.api)
        implementation(projects.components.cloud.api)

        implementation(libs.ktor.client.core)
        implementation(libs.ktor.negotiation)
        implementation(libs.ktor.serialization)
        implementation(libs.ktor.client.websockets)

        implementation(libs.kotlin.coroutines)
        implementation(libs.kotlin.serialization.json)
    }
}
