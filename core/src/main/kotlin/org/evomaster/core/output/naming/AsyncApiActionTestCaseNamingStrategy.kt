package org.evomaster.core.output.naming

import org.evomaster.core.output.TestWriterUtils.safeVariableName
import org.evomaster.core.problem.asyncapi.data.AsyncApiAction
import org.evomaster.core.problem.asyncapi.data.AsyncApiCallResult
import org.evomaster.core.problem.asyncapi.data.AsyncApiOutcome
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Solution
import org.evomaster.core.search.action.EvaluatedAction

/**
 * Names a test after the operation it publishes to and what came of it.
 *
 * There is no status code to name an outcome with, so the name carries the outcome itself, and
 * the declared message a reply was recognised as where there is one: two tests over the same
 * operation are usually told apart by which reply variant they reached.
 */
open class AsyncApiActionTestCaseNamingStrategy(
    solution: Solution<*>,
    languageConventionFormatter: LanguageConventionFormatter,
    maxTestCaseNameLength: Int,
) : ActionTestCaseNamingStrategy(solution, languageConventionFormatter, maxTestCaseNameLength) {

    private val publish = "publish"

    override fun expandName(
        individual: EvaluatedIndividual<*>,
        nameTokens: MutableList<String>,
        ambiguitySolvers: List<AmbiguitySolver>
    ): String {
        val evaluatedAction = individual.evaluatedMainActions().last()
        val action = evaluatedAction.action as AsyncApiAction
        var remainingNameChars = maxTestCaseNameLength - namePrefixChars()

        /*
            Shaped like REST's, which reads verb then target then result. What a tester does to
            an operation is publish to it, whichever direction the document declares.
         */
        remainingNameChars = addNameTokensIfAllowed(
            nameTokens,
            listOf(publish, on, safeVariableName(action.operationId)),
            remainingNameChars
        )
        addResult(individual, nameTokens, remainingNameChars)

        return formatName(nameTokens)
    }

    override fun resolveAmbiguities(duplicatedIndividuals: Set<EvaluatedIndividual<*>>): Map<EvaluatedIndividual<*>, String> {
        return emptyMap()
    }

    override fun addActionResult(
        evaluatedAction: EvaluatedAction,
        nameTokens: MutableList<String>,
        remainingNameChars: Int
    ): Int {
        val result = evaluatedAction.result as AsyncApiCallResult

        val candidateTokens = when (result.getOutcome()) {
            AsyncApiOutcome.REPLIED -> {
                val message = result.getReplyMessage()
                if (message != null) {
                    mutableListOf(returns, safeVariableName(message))
                } else {
                    /*
                        A reply arrived that matches none of the declared messages. Worded to
                        match the fault's own label, for the same reason as below: with the
                        experimental oracles off this names the test, with them on the fault
                        does, and the two must read alike.
                     */
                    mutableListOf("replies", "with", "undeclared", "message")
                }
            }
            //nothing is declared to come back, so reaching the broker is the whole outcome
            AsyncApiOutcome.PUBLISHED -> mutableListOf(returns, "nothing")
            /*
                Worded to match the fault's own label, since a detected fault names the test
                instead of this. With the experimental oracles off there is no fault and this is
                what names it, and a name must not change with a flag that is about reporting.
             */
            AsyncApiOutcome.NO_REPLY -> mutableListOf("gets", "no", "reply")
            AsyncApiOutcome.PUBLISH_FAILED, null -> mutableListOf(error)
        }

        return addNameTokensIfAllowed(nameTokens, candidateTokens, remainingNameChars)
    }
}
