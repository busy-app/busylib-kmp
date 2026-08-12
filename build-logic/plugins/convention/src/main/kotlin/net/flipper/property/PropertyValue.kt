package net.flipper.property

import org.gradle.api.Project

interface PropertyValue {
    val key: String
    fun getValue(): Result<String>
}

private const val CACHE_SERVICE_NAME = "flipperBuildPropertyCache"

fun PropertyValue.asCached(project: Project): PropertyValue {
    val cache = project.gradle.sharedServices
        .registerIfAbsent(CACHE_SERVICE_NAME, BuildPropertyCacheService::class.java) {}
        .get()
    return CachedPropertyValue(
        cache = cache,
        propertyValue = this
    )
}
