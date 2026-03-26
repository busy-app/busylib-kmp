package net.flipper.detekt.rules

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.psiUtil.isPublic

class ApiWrappedTypeRule(config: Config) : Rule(
    config,
    "Public API members in 'api' modules must use wrapped types " +
        "(WrappedFlow, WrappedStateFlow, WrappedSharedFlow, CResult) " +
        "instead of raw Flow, StateFlow, SharedFlow, Result. " +
        "Also, functions returning flow types must not be suspend."
) {
    private val ignoredPaths: List<String> =
        config.valueOrDefault("ignoredPaths", emptyList())

    override fun visitKtFile(file: KtFile) {
        if (shouldCheck(file)) {
            super.visitKtFile(file)
        }
    }

    override fun visitNamedFunction(function: KtNamedFunction) {
        super.visitNamedFunction(function)
        if (!function.isPublic) return

        val typeRef = function.typeReference ?: return
        val typeName = extractSimpleTypeName(typeRef)

        checkForbiddenType(typeName, typeRef, function.name, "function")
        checkSuspendWithFlow(typeName, function)
    }

    override fun visitProperty(property: KtProperty) {
        super.visitProperty(property)
        if (!property.isPublic) return

        val typeRef = property.typeReference ?: return
        val typeName = extractSimpleTypeName(typeRef)

        checkForbiddenType(typeName, typeRef, property.name, "property")
    }

    private fun shouldCheck(file: KtFile): Boolean {
        val filePath = file.virtualFilePath.replace("\\", "/")

        if (isIgnoredPath(filePath)) return false
        if (!isFeatureApiModule(filePath)) return false

        return true
    }

    private fun isFeatureApiModule(filePath: String): Boolean {
        val srcIndex = filePath.indexOf("/src/")
        if (srcIndex <= 0) return false
        val modulePath = filePath.substring(0, srcIndex)
        return modulePath.endsWith("/api") && modulePath.contains("/feature/")
    }

    private fun isIgnoredPath(filePath: String): Boolean {
        return ignoredPaths.any { filePath.contains(it) }
    }

    private fun extractSimpleTypeName(typeRef: KtTypeReference): String {
        val text = typeRef.text
        val idx = text.indexOf('<')
        val base = if (idx >= 0) text.substring(0, idx) else text
        return base.substringAfterLast('.').trim().removeSuffix("?")
    }

    private fun checkForbiddenType(
        typeName: String,
        typeRef: KtTypeReference,
        memberName: String?,
        memberKind: String,
    ) {
        if (memberName == null) return
        val replacement = FORBIDDEN_TYPE_REPLACEMENTS[typeName] ?: return
        report(
            Finding(
                Entity.from(typeRef),
                "Public $memberKind '$memberName' in api module returns '$typeName'. " +
                    "Use '$replacement' instead for Swift interoperability."
            )
        )
    }

    private fun checkSuspendWithFlow(typeName: String, function: KtNamedFunction) {
        if (!function.hasModifier(KtTokens.SUSPEND_KEYWORD)) return
        if (typeName in FLOW_WRAPPED_TYPES) {
            report(
                Finding(
                    Entity.from(function),
                    "Public function '${function.name}' in api module is suspend " +
                        "and returns '$typeName'. Functions returning flow types must not be suspend."
                )
            )
        }
    }

    companion object {
        private val FORBIDDEN_TYPE_REPLACEMENTS = mapOf(
            "Flow" to "WrappedFlow",
            "StateFlow" to "WrappedStateFlow",
            "SharedFlow" to "WrappedSharedFlow",
            "Result" to "CResult",
        )

        private val FLOW_WRAPPED_TYPES = setOf(
            "WrappedFlow",
            "WrappedStateFlow",
            "WrappedSharedFlow",
        )
    }
}
