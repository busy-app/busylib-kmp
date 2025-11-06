package com.flipperdevices.buildlogic.model

/**
 * This enum is used to define new kotlin-generated BuildKonfig
 *
 * We already have multiple flavors for android - debug, internal, release
 * but android's flavor BuildConfig generation isn't compatible with KMP,
 * so in the end, when project will e KMP-full, this will be final version
 * of BuildKonfig field values
 */
enum class FlavorType(
    val isLogEnabled: Boolean,
    val isVerboseLogEnabled: Boolean = false,
    val isFileLogEnabled: Boolean,
    val crashAppOnFailedChecks: Boolean,
    val isSentryEnabled: Boolean = true,
    val isSentryPublishMappingsEnabled: Boolean = SENTRY_PUBLISH_ENABLED,
    val isSensitiveLogEnabled: Boolean,
    val isGoogleFeatureAvailable: Boolean,
    val isMockDeviceAvailable: Boolean = false,
    val isDebugSettingsEnabled: Boolean = false,
    val shouldAddAuthTokenToAllRequest: Boolean = true,
    val isDeviceScreenEnabled: Boolean = false,
    val isTestTagEnabled: Boolean = false,
    val isLongPoolingEnabled: Boolean = false
) {
    DEV(
        isLogEnabled = true,
        isFileLogEnabled = true,
        crashAppOnFailedChecks = true,
        isSentryPublishMappingsEnabled = false,
        isSensitiveLogEnabled = true,
        isGoogleFeatureAvailable = true,
        isMockDeviceAvailable = true,
        isDebugSettingsEnabled = true,
        shouldAddAuthTokenToAllRequest = false,
        isDeviceScreenEnabled = true,
        isTestTagEnabled = true
    ),
    PROD_GP(
        // For Google Play
        isLogEnabled = true,
        isFileLogEnabled = false,
        crashAppOnFailedChecks = false,
        isSensitiveLogEnabled = false,
        isGoogleFeatureAvailable = true,
    ),
    INTERNAL_GP(
        // For Google Play
        isLogEnabled = true,
        isFileLogEnabled = false,
        crashAppOnFailedChecks = false,
        isSensitiveLogEnabled = false,
        isGoogleFeatureAvailable = true,
        isDebugSettingsEnabled = true,
        isMockDeviceAvailable = true
    ),
    PROD_GH_GMS(
        // For GitHub, with google services
        isLogEnabled = true,
        isFileLogEnabled = false,
        crashAppOnFailedChecks = false,
        isSensitiveLogEnabled = false,
        isGoogleFeatureAvailable = true,
        isDebugSettingsEnabled = true
    ),
    PROD_GH_NOGMS(
        // For GitHub, without google services
        isLogEnabled = true,
        isFileLogEnabled = false,
        crashAppOnFailedChecks = false,
        isSensitiveLogEnabled = false,
        isGoogleFeatureAvailable = false,
        isDebugSettingsEnabled = true
    )
}

const val SENTRY_PUBLISH_ENABLED = true // Disable sentry publishing if sentry.flipp.dev is down
