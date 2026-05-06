package net.flipper.bridge.connection.feature.firmwareupdate.model

/**
 * @author <a href="https://github.com/flipperdevices/Flipper-Android-App/blob/1dab71479f900742432ad3c7749785aa35004059/components/core/data/src/commonMain/kotlin/com/flipperdevices/core/data/SemVer.kt#L3">Flipper</a>
 */
@Suppress("MaxLineLength")
internal data class SemVer(
    val majorVersion: Int,
    val minorVersion: Int,
    val patchVersion: Int? = null,
    val additionalVersion: Int? = null
) : Comparable<SemVer> {

    override fun compareTo(other: SemVer): Int {
        var comparableNumber = majorVersion.compareTo(other.majorVersion)
        if (comparableNumber != 0) {
            return comparableNumber
        }
        comparableNumber = minorVersion.compareTo(other.minorVersion)
        if (other.patchVersion == null ||
            patchVersion == null ||
            comparableNumber != 0
        ) {
            return comparableNumber
        }

        comparableNumber = patchVersion.compareTo(other.patchVersion)
        if (other.additionalVersion == null ||
            additionalVersion == null ||
            comparableNumber != 0
        ) {
            return comparableNumber
        }

        return additionalVersion.compareTo(other.additionalVersion)
    }

    override fun toString(): String {
        return when {
            additionalVersion != null -> "$majorVersion.$minorVersion.$patchVersion.$additionalVersion"
            patchVersion != null -> "$majorVersion.$minorVersion.$patchVersion"
            else -> "$majorVersion.$minorVersion"
        }
    }

    companion object {
        fun fromString(version: String): SemVer? {
            val versionParts = version.split(".")
            if (versionParts.size < 2 || versionParts.size > 4) {
                return null
            }
            return runCatching {
                SemVer(
                    majorVersion = versionParts[0].toInt(),
                    minorVersion = versionParts[1].toInt(),
                    patchVersion = versionParts.getOrNull(2)?.toInt(),
                    additionalVersion = versionParts.getOrNull(3)?.toInt()
                )
            }.getOrNull()
        }
    }
}
