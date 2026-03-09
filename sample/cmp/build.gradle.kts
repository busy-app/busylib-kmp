import net.flipper.property.SecretPropertyValue
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.konan.target.Family


plugins {
    id("flipper.multiplatform-compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    alias(libs.plugins.skie)
    alias(libs.plugins.buildkonfig)
}

afterEvaluate {
    tasks.withType<AbstractPublishToMaven>().configureEach { enabled = false }
}


buildConfig {
    className("SampleKonfig")
    packageName("${kotlin.android.namespace}")
    buildConfigField(
        String::class.java,
        "SECRET_AUTH_TOKEN",
        SecretPropertyValue(project, "flipper.authToken")
            .getValue()
            .getOrNull() ?: ""
    )
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
        implementation(projects.components.core.ktor)

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

        implementation(libs.ktor.client.core)
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
