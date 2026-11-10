package net.flipper.core.busylib.data.semver.util

import net.flipper.core.busylib.data.semver.model.SemVer

private const val MIN_PART_COUNT = 2
private const val MAX_PART_COUNT = 4
private const val PATCH_INDEX = 2
private const val ADDITIONAL_INDEX = 3

fun String.toSemVerOrNull(): SemVer? {
    val parts = substringBefore('-')
        .substringBefore('+')
        .split('.')
    if (parts.size !in MIN_PART_COUNT..MAX_PART_COUNT) return null
    val numbers = parts.map { part -> part.toIntOrNull() ?: return null }
    return SemVer(
        majorVersion = numbers[0],
        minorVersion = numbers[1],
        patchVersion = numbers.getOrNull(PATCH_INDEX) ?: 0,
        additionalVersion = numbers.getOrNull(ADDITIONAL_INDEX) ?: 0
    )
}
