package org.evomaster.core.output.naming

import org.evomaster.core.problem.httpws.HttpWsCallResult
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.problem.rest.RestIndividual
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Solution

class ActionTestCaseNamingStrategy(
    solution: Solution<*>
) : NumberedTestCaseNamingStrategy(solution)  {


    override fun expandName(individual: EvaluatedIndividual<*>): String {
        if (solution.individuals.any { it.individual is RestIndividual }) {
            var evaluatedAction = individual.evaluatedMainActions().last()
            var action = evaluatedAction.action as RestCallAction
            var result = evaluatedAction.result as HttpWsCallResult
            return "_${action.verb}_on_${action.path.nameQualifier}_returns_${result.getStatusCode()}"
        } else {
            return "_wasNotRestIndividual"
        }
    }

}
