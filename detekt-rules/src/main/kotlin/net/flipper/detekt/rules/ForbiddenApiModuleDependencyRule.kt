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

    override fun visitKtFile(file: KtFile) {
        if (shouldCheck(file)) {
            super.visitKtFile(file)
        }
    }

    override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
        if (expression.text == FORBIDDEN_DEPENDENCY) {
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

    private fun shouldCheck(file: KtFile): Boolean {
        val filePath = file.virtualFilePath.replace("\\", "/")
        val parentDirName = File(filePath).parentFile?.name
        return parentDirName == "api" && ignoredPaths.none { filePath.contains(it) }
    }

    companion object {
        const val FORBIDDEN_DEPENDENCY = "projects.components.bridge.feature.rpc.api"
    }
}
