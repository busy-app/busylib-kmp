package net.flipper.core.busylib.data.semver.model

/**
 * @author <a href="https://github.com/flipperdevices/Flipper-Android-App/blob/1dab71479f900742432ad3c7749785aa35004059/components/core/data/src/commonMain/kotlin/com/flipperdevices/core/data/SemVer.kt#L3">Flipper</a>
 */
@Suppress("MaxLineLength")
data class SemVer(
    val majorVersion: Int,
    val minorVersion: Int,
    val patchVersion: Int,
    val additionalVersion: Int
) : Comparable<SemVer> {

    override fun compareTo(other: SemVer): Int {
        return compareValuesBy(
            this,
            other,
            SemVer::majorVersion,
            SemVer::minorVersion,
            SemVer::patchVersion,
            SemVer::additionalVersion
        )
    }

    override fun toString(): String {
        val version = "$majorVersion.$minorVersion.$patchVersion"
        if (additionalVersion == 0) return version
        return "$version.$additionalVersion"
    }
}
