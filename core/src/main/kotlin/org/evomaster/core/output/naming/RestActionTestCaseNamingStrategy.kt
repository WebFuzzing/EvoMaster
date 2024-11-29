package org.evomaster.core.output.naming

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import org.evomaster.core.problem.rest.HttpVerb
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.problem.rest.RestCallResult
import org.evomaster.core.problem.rest.RestPath
import org.evomaster.core.problem.rest.param.QueryParam
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Solution
import org.evomaster.core.search.action.Action
import org.evomaster.core.search.action.EvaluatedAction
import org.evomaster.core.search.gene.BooleanGene
import org.evomaster.core.search.gene.numeric.NumberGene
import org.evomaster.core.search.gene.string.StringGene
import javax.ws.rs.core.MediaType

open class RestActionTestCaseNamingStrategy(
    solution: Solution<*>,
    languageConventionFormatter: LanguageConventionFormatter,
    private val nameWithQueryParameters: Boolean,
) : ActionTestCaseNamingStrategy(solution, languageConventionFormatter)  {

    override fun expandName(individual: EvaluatedIndividual<*>, nameTokens: MutableList<String>, ambiguitySolver: ((Action) -> List<String>)?): String {
        val evaluatedAction = individual.evaluatedMainActions().last()
        val action = evaluatedAction.action as RestCallAction

        nameTokens.add(action.verb.toString().lowercase())
        nameTokens.add(on)
        if (ambiguitySolver == null) {
            nameTokens.add(getPath(action.path.nameQualifier))
        } else {
            nameTokens.addAll(ambiguitySolver(action))
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
        val solvedAmbiguities = mutableMapOf<EvaluatedIndividual<*>, String>()
        val workingCopy = duplicatedIndividuals.toMutableSet()

        val pathDisambiguatedIndividuals = solvePathAmbiguities(workingCopy)
        solvedAmbiguities.putAll(pathDisambiguatedIndividuals)
        removeSolvedDuplicates(workingCopy, pathDisambiguatedIndividuals.keys)

        val queryParamsDisambiguatedIndividuals = solveQueryParamsAmbiguities(workingCopy)
        solvedAmbiguities.putAll(queryParamsDisambiguatedIndividuals)
        removeSolvedDuplicates(workingCopy, queryParamsDisambiguatedIndividuals.keys)

        return solvedAmbiguities
    }

    /*
     * If any test has a different path, then add previous segment to them to make them differ.
     * The filter call ensures that we are only performing this disambiguation when there's only one individual that
     * differs and when said individual does not have a parameter as a last element since it might differ in the
     * parameter name but not the rest of the path.
     */
    private fun solvePathAmbiguities(duplicatedIndividuals: MutableSet<EvaluatedIndividual<*>>): Map<EvaluatedIndividual<*>, String> {
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
            .filter { it.value.size == 1 && !it.key.second }
            .mapNotNull { entry ->
                val eInd = entry.value[0]
                eInd to expandName(eInd, mutableListOf(), ::pathAmbiguitySolver)
            }
            .toMap()
    }

    /*
     * If the last element of a path is a parameter then we must go up a level
     *
     * Example: /products/{productName}/configurations/{configurationName}/features/{featureName}
     * must now include the name qualifier for configurations
     */
    private fun pathAmbiguitySolver(action: Action): List<String> {
        val restAction = action as RestCallAction
        val lastPath = restAction.path
        var parentPath = restAction.path.parentPath()
        if (lastPath.isLastElementAParameter()) {
            parentPath = parentPath.parentPath()
        }
        return listOf(getParentPathQualifier(parentPath), getPath(restAction.path.nameQualifier))
    }

    /*
     * If the parent path name qualifier is not root, then we make sure we obtain the sanitized version of it.
     * Otherwise, we'll keep the original path returning the empty string.
     */
    private fun getParentPathQualifier(parentPath: RestPath): String {
        val parentPathQualifier = parentPath.nameQualifier
        return if (parentPathQualifier == "/") "" else getPath(parentPathQualifier)
    }

    /*
     * If any test uses query params, then add withQueryParam(s) after the path to them to make them differ.
     * The filter call ensures that we are only performing this disambiguation when there's only one individual that
     * differs and the list of query params is not empty.
     */
    private fun solveQueryParamsAmbiguities(duplicatedIndividuals: MutableSet<EvaluatedIndividual<*>>): Map<EvaluatedIndividual<*>, String> {
        return duplicatedIndividuals
            .groupBy {
                (it.evaluatedMainActions().last().action as RestCallAction).parameters.filterIsInstance<QueryParam>()
            }
            .filter { it.value.size == 1 && it.key.isNotEmpty()}
            .mapNotNull { entry ->
                val eInd = entry.value[0]
                eInd to expandName(eInd, mutableListOf(), ::queryParamsAmbiguitySolver)
            }
            .toMap()
    }

    /*
     * If there are more than one query parameters, then use plural.
     */
    private fun queryParamsAmbiguitySolver(action: Action): List<String> {
        val restAction = action as RestCallAction
        val result = mutableListOf<String>()
        result.add(getPath(restAction.path.nameQualifier))

        val queryParams = restAction.parameters.filterIsInstance<QueryParam>()
        result.add(with)
        result.add(if (queryParams.size > 1) "${queryParam}s" else queryParam)
        if (nameWithQueryParameters) {
            addQueryParameterNames(queryParams, result)
        }
        return result
    }

    private fun addQueryParameterNames(queryParams: List<QueryParam>, result: MutableList<String>) {
        val booleanQueryParams = getBooleanQueryParams(queryParams)
        booleanQueryParams.forEachIndexed { index, queryParam ->
            result.add(queryParam.name)
            if (index != booleanQueryParams.lastIndex) {
                result.add(and)
            }
        }

        val numberQueryParams = getNumberQueryParams(queryParams)
        numberQueryParams.forEachIndexed { index, queryParam ->
            result.add("negative")
            result.add(queryParam.name)
            if (index != numberQueryParams.lastIndex) {
                result.add(and)
            }
        }

        val emptyStringQueryParams = getEmptyStringQueryParams(queryParams)
        emptyStringQueryParams.forEachIndexed { index, queryParam ->
            result.add("empty")
            result.add(queryParam.name)
            if (index != emptyStringQueryParams.lastIndex) {
                result.add(and)
            }
        }
    }

    private fun getBooleanQueryParams(queryParams: List<QueryParam>): List<QueryParam> {
        return queryParams.filter { it.getGeneForQuery() is BooleanGene && (it.getGeneForQuery() as BooleanGene).value }
    }

    private fun getNumberQueryParams(queryParams: List<QueryParam>): List<QueryParam> {
        return queryParams.filter { it.getGeneForQuery() is NumberGene<*> && (it.getGeneForQuery() as NumberGene<*>).value.toLong() < 0 }
    }

    private fun getEmptyStringQueryParams(queryParams: List<QueryParam>): List<QueryParam> {
        return queryParams.filter { it.getGeneForQuery() is StringGene && (it.getGeneForQuery() as StringGene).getValueAsRawString().trim().isEmpty() }
    }

    private fun removeSolvedDuplicates(duplicatedIndividuals: MutableSet<EvaluatedIndividual<*>>, disambiguatedIndividuals: Set<EvaluatedIndividual<*>>) {
        duplicatedIndividuals.removeAll(disambiguatedIndividuals)
    }

    private fun isGetCall(evaluatedAction: EvaluatedAction): Boolean {
        return (evaluatedAction.action as RestCallAction).verb == HttpVerb.GET
    }

    private fun addBodyShape(result: RestCallResult): List<String> {
        val bodyString = result.getBody()
        if (bodyString.isNullOrBlank()) {
            return listOf(empty)
        }
        val type = result.getBodyType()?: MediaType.TEXT_PLAIN_TYPE
        if (type.isCompatible(MediaType.APPLICATION_JSON_TYPE) || type.toString().toLowerCase().contains("+json")) {
            return handleJsonBody(bodyString)
        }
        return listOf("content")
    }

    private fun handleJsonBody(bodyString: String?): List<String> {
        return when (bodyString?.trim()?.first()) {
            '[' -> getListBodyName(bodyString)
            '{' -> getObjectBodyName(bodyString)
            '"' -> listOf("string")
            else -> listOf("content") // no particular knowledge about the content
        }
    }

    private fun getObjectBodyName(bodyString: String?): List<String> {
        try {
            val objectSize = Gson().fromJson(bodyString, Map::class.java).size
            if (objectSize == 0) {
                return listOf(empty, "object")
            }
        } catch (_: JsonSyntaxException) {
            // If there's any exception, just default to the object name. Test assertion will be handled when writing the actual test
        }
        return listOf("object")
    }

    private fun getListBodyName(bodyString: String?): List<String> {
        return try {
            when (val listSize = Gson().fromJson(bodyString, List::class.java).size) {
                0 -> listOf(empty, "list")
                1 -> listOf("1", "element")
                else -> listOf(listSize.toString(), "elements")
            }
        } catch (e: JsonSyntaxException) {
            // If there's any exception, just default to the list name. Test assertion will be handled when writing the actual test
            listOf("list")
        }
    }

}
