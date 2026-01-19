import io.gitlab.arturbosch.detekt.Detekt
import net.flipper.Config.CURRENT_FLAVOR_TYPE

plugins {
    id("flipper.multiplatform")
    id("flipper.anvil-multiplatform")
    id("ru.astrainteractive.gradleplugin.java.core")
    // id("ru.astrainteractive.gradleplugin.android.namespace") // Temporarily disabled for AGP 9.0.0 compatibility
    // id("ru.astrainteractive.gradleplugin.android.core") // Temporarily disabled for AGP 9.0.0 compatibility

    alias(libs.plugins.buildkonfig)
}

tasks.withType<Detekt>().configureEach {
    enabled = false
}

buildConfig {
    className("BuildKonfig")
    packageName("net.flipper.busylib.kmp.components.core.buildkonfig")
    useKotlinOutput { internalVisibility = false }
    buildConfigField(Boolean::class.java, "IS_LOG_ENABLED", CURRENT_FLAVOR_TYPE.isLogEnabled)

    buildConfigField(
        Boolean::class.java,
        "IS_VERBOSE_LOG_ENABLED",
        CURRENT_FLAVOR_TYPE.isVerboseLogEnabled
    )
    buildConfigField(
        Boolean::class.java,
        "IS_SENSITIVE_LOG_ENABLED",
        CURRENT_FLAVOR_TYPE.isSensitiveLogEnabled
    )
    buildConfigField(
        Boolean::class.java,
        "CRASH_APP_ON_FAILED_CHECKS",
        CURRENT_FLAVOR_TYPE.crashAppOnFailedChecks
    )
}
