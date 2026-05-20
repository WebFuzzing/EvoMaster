package org.evomaster.core.output.naming

import org.evomaster.core.output.TestWriterUtils.safeVariableName
import org.evomaster.core.problem.asyncapi.data.AsyncAPIAction
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Solution
import org.evomaster.core.search.action.EvaluatedAction

/**
 * AsyncAPI test-case naming strategy (M11-PR2 fix F).
 *
 * Replaces the default `test_0`, `test_1` … naming with a semantic form
 * derived from the individual's action sequence:
 *
 *   testPublishOnStreetlights_returnsOk
 *   testPublishOnDepth_subscribeReply_returnsTimeout
 *   testSubscribeOutputOnVsiV1_returnsCollected
 *
 * The token order follows the REST / GraphQL precedents in
 * [ActionTestCaseNamingStrategy]: the action verb (PUBLISH / SUBSCRIBE_REPLY
 * / SUBSCRIBE_OUTPUT), `on`, the channel address (sanitised), and finally
 * the delivery / reply outcome (`ok`, `fail`, `timeout`, `collected`).
 *
 * Token cap from `maxTestCaseNameLength` is honoured: tokens are dropped
 * tail-first so the most informative prefix survives.
 */
open class AsyncAPIActionTestCaseNamingStrategy(
    solution: Solution<*>,
    languageConventionFormatter: LanguageConventionFormatter,
    maxTestCaseNameLength: Int,
) : ActionTestCaseNamingStrategy(solution, languageConventionFormatter, maxTestCaseNameLength) {

    override fun expandName(individual: EvaluatedIndividual<*>, nameTokens: MutableList<String>, ambiguitySolvers: List<AmbiguitySolver>): String {
        // Bypass [EvaluatedIndividual.evaluatedMainActions]: that helper
        // gates on `EnterpriseActionResult`, which AsyncAPI doesn't use
        // (the fitness layer emits plain `ActionResult`). Reading
        // `seeMainExecutableActions()` directly gives us the action sequence
        // we need for semantic naming without that gating dependency.
        val mainActions = individual.individual.seeMainExecutableActions()
        val firstAsync = mainActions.firstOrNull { it is AsyncAPIAction } as? AsyncAPIAction
            ?: return formatName(nameTokens)
        var remainingNameChars = maxTestCaseNameLength - namePrefixChars()

        val verb = when (firstAsync.kind) {
            AsyncAPIAction.Kind.PUBLISH -> "publish"
            AsyncAPIAction.Kind.SUBSCRIBE_REPLY -> "subscribeReply"
            AsyncAPIAction.Kind.SUBSCRIBE_OUTPUT -> "subscribeOutput"
        }
        val channelToken = safeVariableName(firstAsync.channelAddress)
        remainingNameChars = addNameTokensIfAllowed(
            nameTokens, listOf(verb, on, channelToken), remainingNameChars
        )

        // Surface the recorded delivery / reply outcome — same as the parent
        // strategies, but driven off the raw action result map (no need for
        // the strategy's `addResult` helper which expects an EnterpriseAction).
        val firstAction = mainActions.firstOrNull { it is AsyncAPIAction } ?: return formatName(nameTokens)
        val result = individual.seeResult(firstAction.getLocalId())
        val outcome = result?.getResultValue("delivery") ?: result?.getResultValue("reply")
        if (outcome != null) {
            addNameTokensIfAllowed(nameTokens, listOf(returns, outcome), remainingNameChars)
        }
        return formatName(nameTokens)
    }

    override fun resolveAmbiguities(duplicatedIndividuals: Set<EvaluatedIndividual<*>>): Map<EvaluatedIndividual<*>, String> {
        // Ambiguity resolution falls back to the parent's numbered suffix.
        return emptyMap()
    }

    override fun addActionResult(evaluatedAction: EvaluatedAction, nameTokens: MutableList<String>, remainingNameChars: Int): Int {
        // AsyncAPI action results aren't a uniform type (HttpWsCallResult /
        // GraphQlCallResult): we just describe delivery / reply outcome
        // recorded by the fitness layer in the result's getResultValue map.
        val result = evaluatedAction.result
        val candidateTokens = mutableListOf(returns)
        val outcome = result.getResultValue("delivery") ?: result.getResultValue("reply") ?: "ok"
        candidateTokens.add(outcome)
        return addNameTokensIfAllowed(nameTokens, candidateTokens, remainingNameChars)
    }
}
