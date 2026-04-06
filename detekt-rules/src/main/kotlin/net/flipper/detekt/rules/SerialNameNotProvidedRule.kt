package net.flipper.detekt.rules

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtModifierListOwnerStub

private const val ISSUE_DESCRIPTION =
    "Classes annotated with @Serializable must annotate all serialized fields " +
        "with @SerialName (excluding @Transient fields)."

class SerialNameNotProvidedRule(config: Config) : Rule(
    config,
    ISSUE_DESCRIPTION
) {

    private fun KtAnnotationEntry.shortName(): String? = this.shortName?.asString()

    private fun KtModifierListOwnerStub<*>.hasAnnotation(name: String): Boolean {
        return this.getAnnotationEntries()
            .any { annotationEntry ->
                val annotationName = annotationEntry.shortName()
                annotationName != null && annotationName == name
            }
    }

    override fun visitClass(klass: KtClass) {
        super.visitClass(klass)
        if (!klass.hasAnnotation(SERIALIZABLE_ANNOTATION_NAME)) {
            return
        }

        klass.primaryConstructorParameters
            .filter { property -> property.hasValOrVar() }
            .filter { property -> !property.hasAnnotation(TRANSIENT_ANNOTATION_NAME) }
            .filter { property -> !property.hasAnnotation(REQUIRED_ANNOTATION_NAME) }
            .onEach { property ->
                reportFinding(property)
            }

        klass.getBody()
            ?.properties
            .orEmpty()
            .filter { property -> !property.hasAnnotation(TRANSIENT_ANNOTATION_NAME) }
            .filter { property -> !property.hasAnnotation(REQUIRED_ANNOTATION_NAME) }
            .onEach { property ->
                reportFinding(property)
            }
        klass.getBody()
            ?.enumEntries
            .orEmpty()
            .filter { property -> !property.hasAnnotation(TRANSIENT_ANNOTATION_NAME) }
            .filter { property -> !property.hasAnnotation(REQUIRED_ANNOTATION_NAME) }
            .onEach { property ->
                reportFinding(property)
            }
    }

    private fun reportFinding(element: KtModifierListOwnerStub<*>) {
        report(
            Finding(
                entity = Entity.from(element),
                message = ISSUE_DESCRIPTION
            )
        )
    }

    companion object {
        private const val REQUIRED_ANNOTATION_NAME = "SerialName"
        private const val SERIALIZABLE_ANNOTATION_NAME = "Serializable"
        private const val TRANSIENT_ANNOTATION_NAME = "Transient"
    }
}
