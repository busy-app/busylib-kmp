package net.flipper

/**
 * This enum is used to define new kotlin-generated BuildKonfig
 *
 * We already have multiple flavors for android - debug, internal, release
 * but android's flavor BuildConfig generation isn't compatible with KMP,
 * so in the end, when project will e KMP-full, this will be final version
 * of BuildKonfig field values
 */
@Suppress("LongParameterList")
enum class FlavorType(
    val isLogEnabled: Boolean = false,
    val isVerboseLogEnabled: Boolean = false,
    val crashAppOnFailedChecks: Boolean = false,
    val isSensitiveLogEnabled: Boolean = false,
    val isMockEnabled: Boolean = false,
    val isCloudEnabled: Boolean = false,
    val updateApiChannel: String
) {
    DEBUG(
        isLogEnabled = true,
        crashAppOnFailedChecks = true,
        isSensitiveLogEnabled = true,
        isVerboseLogEnabled = false,
        isMockEnabled = true,
        isCloudEnabled = true,
        updateApiChannel = "development"
    ),
    PROD(
        isLogEnabled = true,
        updateApiChannel = "development"
    ),
}
