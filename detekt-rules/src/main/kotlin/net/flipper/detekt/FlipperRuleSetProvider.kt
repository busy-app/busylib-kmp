package net.flipper.detekt

import dev.detekt.api.RuleSet
import dev.detekt.api.RuleSetId
import dev.detekt.api.RuleSetProvider
import net.flipper.detekt.rules.ForbiddenApiModuleDependencyRule

class FlipperRuleSetProvider : RuleSetProvider {
    override val ruleSetId = RuleSetId("FlipperRule")

    override fun instance(): RuleSet {
        return RuleSet(ruleSetId, listOf(::ForbiddenApiModuleDependencyRule))
    }
}
