package net.flipper.property

import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import java.util.concurrent.ConcurrentHashMap

abstract class BuildPropertyCacheService : BuildService<BuildServiceParameters.None> {
    private val values = ConcurrentHashMap<String, Result<String>>()

    fun computeIfAbsent(key: String, resolve: () -> Result<String>): Result<String> {
        return values.computeIfAbsent(key) { resolve() }
    }
}
