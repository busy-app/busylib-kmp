plugins {
    id("flipper.multiplatform")
    id("flipper.anvil-multiplatform")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.components.bridge.transport.common.api)

        implementation(projects.components.bridge.transport.ble.api)
        implementation(projects.components.bridge.transport.common.impl)

        implementation(projects.components.core.log)
        implementation(projects.components.core.di)
        implementation(projects.components.core.ktx)
        implementation(projects.components.core.wrapper)

        implementation(libs.kotlin.coroutines)
        implementation(libs.kotlin.immutable)
        implementation(libs.ktor.client.core)
        implementation(libs.ktor.client.cio)
    }

    sourceSets.commonTest.dependencies {
        implementation(libs.kotlin.test)
        implementation(libs.kotlin.coroutines.test)
    }

    sourceSets.androidMain.dependencies {
        implementation(libs.ble.client)
        implementation(libs.slf4j.android)
    }

    androidLibrary {
        withHostTest {}
    }

    sourceSets {
        val androidHostTest by getting
        androidHostTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlin.coroutines.test)
            implementation(libs.mockk)
        }
    }
}

// The slf4j-android adapter is meaningful only on a real Android runtime: its <clinit>
// touches android.util.Log, which is unimplemented in the stub android.jar used by
// androidHostTest and throws "Method isLoggable in android.util.Log not mocked" as soon
// as any classpath consumer (e.g. Ktor's HttpClient) requests a logger.
configurations.named("androidHostTestRuntimeClasspath") {
    exclude(group = "uk.uuid.slf4j", module = "slf4j-android")
}
