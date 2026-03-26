package net.flipper.detekt.rules

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtFile
import java.io.File

class ForbiddenApiModuleDependencyRule(config: Config) : Rule(
    config,
    "Modules with ':api' suffix should not depend on $FORBIDDEN_DEPENDENCY directly. " +
        "Use a more specific API module dependency instead."
) {
    private val ignoredPaths: List<String> =
        config.valueOrDefault("ignoredPaths", emptyList())

    private var shouldCheck = false

    override fun visitKtFile(file: KtFile) {
        val filePath = file.virtualFilePath.replace("\\", "/")
        val parentDirName = File(filePath).parentFile?.name
        shouldCheck = parentDirName == "api" && !isIgnored(filePath)
        if (shouldCheck) {
            super.visitKtFile(file)
        }
        shouldCheck = false
    }

    override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
        if (shouldCheck && expression.text == FORBIDDEN_DEPENDENCY) {
            report(
                Finding(
                    Entity.from(expression),
                    "':api' module should not depend on '$FORBIDDEN_DEPENDENCY'. " +
                        "This creates a transitive dependency on the RPC layer from API modules."
                )
            )
        }
        super.visitDotQualifiedExpression(expression)
    }

    private fun isIgnored(filePath: String): Boolean {
        return ignoredPaths.any { filePath.contains(it) }
    }

    companion object {
        const val FORBIDDEN_DEPENDENCY = "projects.components.bridge.feature.rpc.api"
    }
}
