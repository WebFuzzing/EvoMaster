package org.evomaster.core.output.naming.rest

import org.evomaster.core.output.naming.ActionTestCaseNamingStrategy
import org.evomaster.core.output.naming.AmbiguitySolver
import org.evomaster.core.output.naming.LanguageConventionFormatter
import org.evomaster.core.output.naming.rest.RestUtils.addBodyShape
import org.evomaster.core.output.naming.rest.RestUtils.getPath
import org.evomaster.core.output.naming.rest.RestUtils.isGetCall
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.problem.rest.RestCallResult
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Solution
import org.evomaster.core.search.action.EvaluatedAction

open class RestActionTestCaseNamingStrategy(
    solution: Solution<*>,
    languageConventionFormatter: LanguageConventionFormatter,
    private val nameWithQueryParameters: Boolean,
) : ActionTestCaseNamingStrategy(solution, languageConventionFormatter)  {

    override fun expandName(individual: EvaluatedIndividual<*>, nameTokens: MutableList<String>, ambiguitySolvers: List<AmbiguitySolver>): String {
        val evaluatedAction = individual.evaluatedMainActions().last()
        val action = evaluatedAction.action as RestCallAction

        nameTokens.add(action.verb.toString().lowercase())
        nameTokens.add(on)
        if (ambiguitySolvers.isEmpty()) {
            nameTokens.add(getPath(action.path.nameQualifier))
        } else {
            // TODO: max chars check. Idea: if len(name) + len(resultTokens) + len(foreach:solverResult) <= MAX_CHARS ---> OK
            // else keep only name + acceptedSolverResults + resultTokens
            ambiguitySolvers.forEach { solver -> nameTokens.addAll(solver.apply(action)) }
        }
        addResult(individual, nameTokens)

        return formatName(nameTokens)
    }

    override fun addActionResult(evaluatedAction: EvaluatedAction, nameTokens: MutableList<String>) {
        val result = evaluatedAction.result as RestCallResult
        nameTokens.add(returns)
        if (isGetCall(evaluatedAction) && result.getStatusCode() == 200) {
            nameTokens.addAll(addBodyShape(result))
        } else {
            nameTokens.add(result.getStatusCode().toString())
        }
    }

    /**
     * In REST Individuals, ambiguities will be resolved with the path and queryParams solvers.
     * UriParams solver will be left for experimentalPurposes, as well as using the query param values for naming.
     *
     * Whenever an ambiguity is solved, then it should remove that test from the cycle. There is no need to execute the
     * following solvers.
     */
    override fun resolveAmbiguities(duplicatedIndividuals: Set<EvaluatedIndividual<*>>): Map<EvaluatedIndividual<*>, String> {
        val workingCopy = duplicatedIndividuals.toMutableSet()

        val ambiguitySolversPerIndividual = mutableMapOf<EvaluatedIndividual<*>, MutableList<AmbiguitySolver>>()

        val pathDisambiguatedIndividuals = getPathDisambiguationIndividuals(workingCopy)
        pathDisambiguatedIndividuals.forEach {
            ambiguitySolversPerIndividual[it] = mutableListOf(PathAmbiguitySolver())
        }

        val queryParamsDisambiguatedIndividuals = getQueryParamsDisambiguationIndividuals(workingCopy)
        queryParamsDisambiguatedIndividuals.forEach {
            val queryParamsAmbiguitySolver = QueryParamsAmbiguitySolver(nameWithQueryParameters)
            if (ambiguitySolversPerIndividual.containsKey(it)) {
                ambiguitySolversPerIndividual[it]?.add(queryParamsAmbiguitySolver)
            } else {
                ambiguitySolversPerIndividual[it] = mutableListOf(queryParamsAmbiguitySolver)
            }
        }

        return collectSolvedNames(ambiguitySolversPerIndividual)
    }

    /*
     * When two or more individuals share a name, no disambiguation is performed.
     * Otherwise, we would just be increasing test case name length without having actually disambiguated.
     */
    private fun collectSolvedNames(ambiguitySolversPerIndividual: Map<EvaluatedIndividual<*>, MutableList<AmbiguitySolver>>): Map<EvaluatedIndividual<*>, String> {
        return ambiguitySolversPerIndividual
            .map { it.key to expandName(it.key, mutableListOf(), it.value) }
            .groupBy({ it.second }, { it.first })
            .filter { it.value.size == 1 }
            .flatMap { entry -> entry.value.map { key -> key to entry.key } }
            .toMap()
    }

    /*
     * If any test has a different path, then add previous segment to them to make them differ.
     * The filter call ensures that we are only performing this disambiguation when there's only one individual that
     * differs and when said individual does not have a parameter as a last element since it might differ in the
     * parameter name but not the rest of the path.
     */
    private fun getPathDisambiguationIndividuals(duplicatedIndividuals: MutableSet<EvaluatedIndividual<*>>): List<EvaluatedIndividual<*>> {
        return duplicatedIndividuals
            .groupBy {
                var path = (it.evaluatedMainActions().last().action as RestCallAction).path
                if (path.isLastElementAParameter()) {
                    path = path.parentPath()
                }
                val toStringPath = path.toString()
                val isLastAParam = path.isLastElementAParameter()
                Pair(toStringPath, isLastAParam)
            }
            .filter { it.value.isNotEmpty() && !it.key.second }
            .flatMap { it.value }
            .toList()

    }

    /*
     * If any test uses query params, then add withQueryParam(s) after the path to them to make them differ.
     * The filter call ensures that we are only performing this disambiguation when there's only one individual that
     * differs and the list of query params is not empty.
     */
    private fun getQueryParamsDisambiguationIndividuals(duplicatedIndividuals: MutableSet<EvaluatedIndividual<*>>): List<EvaluatedIndividual<*>> {
        return duplicatedIndividuals
            .groupBy {
                val restAction = it.evaluatedMainActions().last().action as RestCallAction
                restAction.path.getOnlyUsableQueries(restAction.parameters)
                    .filter { queryParam ->  queryParam.getGeneForQuery().staticCheckIfImpactPhenotype() }
            }
            .filter { it.value.isNotEmpty() && it.key.isNotEmpty()}
            .flatMap { it.value }
            .toList()
    }

}
