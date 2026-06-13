plugins {
    id("flipper.multiplatform")
    id("flipper.metro-multiplatform")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.components.bridge.feature.about.api)

        implementation(projects.components.core.di)
        implementation(projects.components.core.wrapper)

        implementation(projects.components.bridge.feature.rpc.api)

        implementation(projects.components.bridge.feature.common.api)
        implementation(projects.components.bridge.transport.common.api)

        implementation(libs.kotlin.coroutines)
    }
}
