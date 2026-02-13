import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.konan.target.Family


plugins {
    id("flipper.multiplatform-compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    alias(libs.plugins.skie)
}

afterEvaluate {
    tasks.withType<AbstractPublishToMaven>().configureEach { enabled = false }
}

kotlin {
    targets
        .filterIsInstance<KotlinNativeTarget>()
        .filter { it.konanTarget.family == Family.IOS || it.konanTarget.family == Family.OSX }
        .forEach { target ->
            target.binaries.framework {
                baseName = "BridgeConnection"
                isStatic = true

                export(libs.decompose)
                export(projects.entrypoint)
            }
        }
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.components.core.log)
        implementation(projects.components.core.ktx)
        implementation(projects.components.core.wrapper)

        api(projects.entrypoint)
        implementation(projects.components.bridge.config.impl)

        implementation(libs.settings)
        implementation(libs.settings.observable)
        implementation(libs.settings.coroutines)
        implementation(libs.kotlin.coroutines)
        implementation(libs.kotlin.immutable)
        implementation(libs.kotlin.serialization.json)
        api(libs.decompose)
        implementation(libs.decompose.composeExtension)
        implementation(libs.klibs.kstorage)
    }

    sourceSets.jvmMain.dependencies {
        implementation(libs.decompose.composeExtension)
    }

    sourceSets.androidMain.dependencies {
        implementation(libs.timber)
        implementation(libs.ble.client)
        implementation(libs.androidx.activity.compose)
        implementation(libs.appcompat)
    }
    sourceSets.jvmMain.dependencies {
        implementation(projects.components.core.ktor)

        implementation(libs.ktor.client.core)

        implementation(compose.desktop.currentOs)
    }
}

compose.desktop {
    application {
        mainClass = "net.flipper.bridge.connection.AppKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "BUSYLib"
            packageVersion = "1.0.0"
        }
    }
}
