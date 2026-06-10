plugins {
    id("flipper.multiplatform")
    id("flipper.anvil-multiplatform")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.components.eventbus.api)
        implementation(projects.components.eventbus.internal)

        implementation(projects.components.core.di)
        implementation(projects.components.core.log)
        implementation(projects.components.core.wrapper)

        implementation(libs.kotlin.coroutines)
    }

    sourceSets.commonTest.dependencies {
        implementation(libs.kotlin.test)
        implementation(libs.kotlin.coroutines.test)
    }
}
