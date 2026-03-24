plugins {
    id("flipper.multiplatform")
    id("flipper.anvil-multiplatform")
}
kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.components.watchers.api)

        implementation(projects.components.core.di)
        implementation(projects.components.core.log)
        implementation(projects.components.core.ktx)

        implementation(projects.components.bridge.config.internal)

        implementation(libs.kotlin.coroutines)
    }

    sourceSets.commonTest.dependencies {
        implementation(libs.kotlin.test)
    }
}
