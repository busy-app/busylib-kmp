import io.gitlab.arturbosch.detekt.Detekt
import net.flipper.Config.CURRENT_FLAVOR_TYPE

plugins {
    id("flipper.multiplatform")
    id("flipper.anvil-multiplatform")

    alias(libs.plugins.buildkonfig)
}

tasks.withType<Detekt>().configureEach {
    enabled = false
}

buildConfig {
    className("BuildKonfig")
    packageName("${kotlin.android.namespace}")
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
