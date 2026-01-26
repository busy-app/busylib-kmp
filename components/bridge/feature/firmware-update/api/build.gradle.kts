plugins {
    id("flipper.multiplatform")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.components.core.ktx)

        api(projects.components.bridge.feature.common.api)
        api(projects.components.bridge.feature.rpc.api)
        implementation(projects.components.core.wrapper)

        implementation(libs.kotlin.coroutines)
        implementation(libs.kotlin.immutable)
    }
}
