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
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.numeric.NumberGene
import org.evomaster.core.search.gene.optional.OptionalGene
import org.evomaster.core.search.gene.string.StringGene
import javax.ws.rs.core.MediaType

open class RestActionTestCaseNamingStrategy(
    solution: Solution<*>,
    languageConventionFormatter: LanguageConventionFormatter,
    private val nameWithQueryParameters: Boolean,
) : ActionTestCaseNamingStrategy(solution, languageConventionFormatter)  {

    override fun expandName(individual: EvaluatedIndividual<*>, nameTokens: MutableList<String>, ambiguitySolvers: List<(Action) -> List<String>>): String {
        val evaluatedAction = individual.evaluatedMainActions().last()
        val action = evaluatedAction.action as RestCallAction

        nameTokens.add(action.verb.toString().lowercase())
        nameTokens.add(on)
        if (ambiguitySolvers.isEmpty()) {
            nameTokens.add(getPath(action.path.nameQualifier))
        } else {
            // TODO: max chars check. Idea: if len(name) + len(resultTokens) + len(foreach:solverResult) <= MAX_CHARS ---> OK
            // else keep only name + acceptedSolverResults + resultTokens
            ambiguitySolvers.forEach { solver -> nameTokens.addAll(solver(action)) }
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

        val ambiguitySolversPerIndividual = mutableMapOf<EvaluatedIndividual<*>, MutableList<(Action) -> List<String>>>()

        val pathDisambiguatedIndividuals = getPathDisambiguationIndividuals(workingCopy)
        pathDisambiguatedIndividuals.forEach {
            ambiguitySolversPerIndividual[it] = mutableListOf(::pathAmbiguitySolver)
        }

        val queryParamsDisambiguatedIndividuals = getQueryParamsDisambiguationIndividuals(workingCopy)
        queryParamsDisambiguatedIndividuals.forEach {
            if (ambiguitySolversPerIndividual.containsKey(it)) {
                ambiguitySolversPerIndividual[it]?.add(::queryParamsAmbiguitySolver)
            } else {
                ambiguitySolversPerIndividual[it] = mutableListOf(::queryParamsAmbiguitySolver)
            }
        }

        return collectSolvedNames(ambiguitySolversPerIndividual)
    }

    /*
     * When two or more individuals share a name, no disambiguation is performed.
     * Otherwise, we would just be increasing test case name length without having actually disambiguated.
     */
    private fun collectSolvedNames(ambiguitySolversPerIndividual: MutableMap<EvaluatedIndividual<*>, MutableList<(Action) -> List<String>>>): Map<EvaluatedIndividual<*>, String> {
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
    private fun getQueryParamsDisambiguationIndividuals(duplicatedIndividuals: MutableSet<EvaluatedIndividual<*>>): List<EvaluatedIndividual<*>> {
        return duplicatedIndividuals
            .groupBy {
                val restAction = it.evaluatedMainActions().last().action as RestCallAction
                restAction.path.getOnlyQuery(restAction.parameters)
            }
            .filter { it.value.isNotEmpty() && it.key.isNotEmpty()}
            .flatMap { it.value }.toList()
    }

    /*
     * If there are more than one query parameters, then use plural.
     */
    private fun queryParamsAmbiguitySolver(action: Action): List<String> {
        val restAction = action as RestCallAction
        val result = mutableListOf<String>()

        val queryParams = restAction.path.getOnlyQuery(restAction.parameters)
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

        val numberQueryParams = getNegativeNumberQueryParams(queryParams)
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

    // TODO: need to check if the BooleanGene is not enclosed in an OptionalGene
    private fun getBooleanQueryParams(queryParams: List<QueryParam>): List<QueryParam> {
        return queryParams.filter {
            val wrappedGene = getWrappedGene(it)
            wrappedGene is BooleanGene && wrappedGene.value
        }
    }

    // TODO: need to check if the NumberGene is not enclosed in an OptionalGene
    private fun getNegativeNumberQueryParams(queryParams: List<QueryParam>): List<QueryParam> {
        return queryParams.filter {
            val wrappedGene = getWrappedGene(it)
            wrappedGene is NumberGene<*> && wrappedGene.value.toLong() < 0
        }
    }

    // TODO: need to check if the StringGene is not enclosed in an OptionalGene
    private fun getEmptyStringQueryParams(queryParams: List<QueryParam>): List<QueryParam> {
        return queryParams.filter {
            val wrappedGene = getWrappedGene(it)
            wrappedGene is StringGene && wrappedGene.getValueAsRawString().trim().isEmpty()
        }
    }

    private fun getWrappedGene(queryParam: QueryParam): Gene {
        return (queryParam.getGeneForQuery() as OptionalGene).gene
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
