plugins {
    id("flipper.multiplatform")
    id("flipper.anvil-multiplatform")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.components.bridge.feature.events.api)

        implementation(projects.components.core.di)
        implementation(projects.components.core.ktx)
        implementation(projects.components.core.log)
        implementation(projects.components.core.buildkonfig)

        implementation(projects.components.bridge.feature.common.api)
        implementation(projects.components.bridge.feature.rpc.api)
        implementation(projects.components.bridge.transport.common.api)
        implementation(projects.components.bridge.bsbprotobuf)

        implementation(libs.kotlin.coroutines)
        implementation(libs.kotlin.datetime)
    }
    sourceSets.commonTest.dependencies {
        implementation(libs.kotlin.test)
    }
}
