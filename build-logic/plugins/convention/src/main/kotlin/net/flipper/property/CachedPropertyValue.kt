package net.flipper.property

import net.flipper.property.exception.PropertyValueNotPresentException
import net.flipper.property.model.EmptyValue
import org.gradle.api.plugins.ExtensionContainer

class CachedPropertyValue(
    private val extensionContainer: ExtensionContainer,
    private val propertyValue: PropertyValue
) : PropertyValue {
    override val key: String = propertyValue.key

    override fun getValue(): Result<String> {
        val extensionValue = extensionContainer.findByName(key)
        if (extensionValue == EmptyValue) {
            return Result.failure(PropertyValueNotPresentException())
        }
        if (extensionValue != null) return Result.success(extensionValue.toString())
        return propertyValue.getValue()
            .onSuccess { value -> extensionContainer.add(key, value) }
            .onFailure { extensionContainer.add(key, EmptyValue) }
    }
}
