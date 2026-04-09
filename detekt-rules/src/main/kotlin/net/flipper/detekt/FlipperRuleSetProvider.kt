package net.flipper.detekt

import dev.detekt.api.RuleSet
import dev.detekt.api.RuleSetId
import dev.detekt.api.RuleSetProvider
import net.flipper.detekt.rules.ApiWrappedTypeRule
import net.flipper.detekt.rules.FilterIsInstanceWithGenericsRule
import net.flipper.detekt.rules.ForbiddenApiModuleDependencyRule
import net.flipper.detekt.rules.SerialNameNotProvidedRule

class FlipperRuleSetProvider : RuleSetProvider {
    override val ruleSetId = RuleSetId("FlipperRule")

    override fun instance(): RuleSet {
        return RuleSet(
            id = ruleSetId,
            rules = listOf(
                ::ForbiddenApiModuleDependencyRule,
                ::ApiWrappedTypeRule,
                ::SerialNameNotProvidedRule,
                ::FilterIsInstanceWithGenericsRule
            )
        )
    }
}
