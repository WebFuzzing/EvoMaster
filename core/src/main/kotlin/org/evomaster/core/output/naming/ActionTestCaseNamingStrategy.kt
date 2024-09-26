package org.evomaster.core.output.naming

import org.evomaster.core.problem.httpws.HttpWsCallResult
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Solution

open class ActionTestCaseNamingStrategy(
    solution: Solution<*>
) : NumberedTestCaseNamingStrategy(solution)  {


    override fun expandName(individual: EvaluatedIndividual<*>): String {
        var evaluatedAction = individual.evaluatedMainActions().last()
        var action = evaluatedAction.action as RestCallAction
        var result = evaluatedAction.result as HttpWsCallResult
        return "_${action.verb}_on_${sanitizePathName(action.path.nameQualifier)}_returns_${result.getStatusCode()}"
    }

    private fun sanitizePathName(nameQualifier: String): String {
        if (nameQualifier == "/") {
            return "root"
        }
        return nameQualifier.replace(".", "").replace("/", "").replace("-","")
    }

}
