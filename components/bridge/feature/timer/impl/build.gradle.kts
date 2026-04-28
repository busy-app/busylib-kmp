plugins {
    id("flipper.multiplatform")
    id("flipper.anvil-multiplatform")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.components.bridge.feature.timer.api)

        implementation(projects.components.core.di)
        implementation(projects.components.core.wrapper)

        implementation(projects.components.bridge.feature.common.api)
        implementation(projects.components.bridge.transport.common.api)

        implementation(libs.kotlin.coroutines)
    }

    sourceSets.commonTest.dependencies {
        implementation(libs.kotlin.test)
        implementation(libs.kotlin.coroutines.test)
    }
}
