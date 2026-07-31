plugins {
    id("flipper.multiplatform")
    id("flipper.metro-multiplatform")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.components.bridge.feature.storage.api)

        implementation(projects.components.core.di)
        implementation(projects.components.core.ktx)
        implementation(projects.components.core.log)

        implementation(projects.components.bridge.feature.common.api)
        implementation(projects.components.bridge.transport.common.api)

        implementation(projects.components.bridge.feature.rpc.api)

        implementation(libs.kotlin.coroutines)
        implementation(libs.kotlin.io)
    }

    sourceSets.commonTest.dependencies {
        implementation(projects.components.bridge.feature.rpc.api)

        implementation(libs.kotlin.test)
        implementation(libs.kotlin.coroutines)
        implementation(libs.kotlin.coroutines.test)
        implementation(libs.kotlin.io)
    }
}
