plugins {
    id("flipper.multiplatform")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.components.core.ktx)
        implementation(projects.components.core.wrapper)

        api(projects.components.bridge.feature.common.api)
        api(projects.components.bridge.feature.rpc.api)

        implementation(libs.kotlin.coroutines)
        implementation(libs.kotlin.immutable)
    }
}
