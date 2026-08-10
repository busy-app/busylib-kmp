package net.flipper.property

import net.flipper.property.exception.PropertyValueNotPresentException

class CachedPropertyValue(
    private val cache: BuildPropertyCacheService,
    private val propertyValue: PropertyValue
) : PropertyValue {
    override val key: String = propertyValue.key

    override fun getValue(): Result<String> {
        var resolvedHere = false
        val value = cache.computeIfAbsent(key) {
            resolvedHere = true
            propertyValue.getValue()
        }
        return when {
            resolvedHere || value.isSuccess -> value
            else -> Result.failure(PropertyValueNotPresentException())
        }
    }
}
