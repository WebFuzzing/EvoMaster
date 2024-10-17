package org.evomaster.core.output.naming

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import org.evomaster.core.EMConfig
import org.evomaster.core.output.TestWriterUtils
import org.evomaster.core.output.TestWriterUtils.safeVariableName
import org.evomaster.core.problem.rest.HttpVerb
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.problem.rest.RestCallResult
import org.evomaster.core.problem.rest.param.PathParam
import org.evomaster.core.problem.rest.param.QueryParam
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Solution
import org.evomaster.core.search.action.Action
import org.evomaster.core.search.action.EvaluatedAction
import java.util.Collections.singletonList
import javax.ws.rs.core.MediaType

open class RestActionTestCaseNamingStrategy(
    solution: Solution<*>,
    languageConventionFormatter: LanguageConventionFormatter
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
     * In REST Individuals. Ambiguity will be resolved with the following filters:
     * 1. if any test has a different path, then add previous segment to them to make them differ
     * 2. if any test uses URI params, add withUriParams{listOfParamValues} to name
     * 3. if any test uses query params, add withQueryParams{listOfParamValues} to name
     *
     * Whenever an ambiguity is solved, then it should remove that test from the cycle. There is no need to execute the
     * following filters
     */
    override fun resolveAmbiguity(individualToName: MutableMap<EvaluatedIndividual<*>, String>, inds: MutableSet<EvaluatedIndividual<*>>) {
        checkForPath(individualToName, inds)
        checkForUriParams(individualToName, inds)
        checkForQueryParams(individualToName, inds)
    }

    private fun checkForPath(individualToName: MutableMap<EvaluatedIndividual<*>, String>, inds: MutableSet<EvaluatedIndividual<*>>) {
        val groupByPath = inds.groupBy {
            var path = (it.evaluatedMainActions().last().action as RestCallAction).path
            if (path.isLastElementAParameter()) {
                path = path.parentPath()
            }
            val toStringPath = path.toString()
            val isLastAParam = path.isLastElementAParameter()
            Pair(toStringPath, isLastAParam)
        }.filter { it.value.size == 1 && !it.key.second }

        groupByPath.forEach { entry ->
            val eInd = entry.value[0]
            individualToName[eInd] = expandName(eInd, mutableListOf(), ::pathAmbiguitySolver)
            inds.remove(eInd)
        }
    }

    private fun pathAmbiguitySolver(action: Action): List<String> {
        val restAction = action as RestCallAction
        val lastPath = restAction.path
        var parentPath = restAction.path.parentPath()
        if (lastPath.isLastElementAParameter()) {
            parentPath = parentPath.parentPath()
        }
        val ppathQuali = parentPath.nameQualifier
        return listOf(ppathQuali, getPath(restAction.path.nameQualifier))
    }

    private fun checkForUriParams(individualToName: MutableMap<EvaluatedIndividual<*>, String>, inds: MutableSet<EvaluatedIndividual<*>>) {
        val groupByUriParams = inds.groupBy {
            (it.evaluatedMainActions().last().action as RestCallAction).parameters.filterIsInstance<PathParam>()
        }.filter { it.value.size == 1 }

        groupByUriParams.forEach { entry ->
            val eInd = entry.value[0]
            individualToName[eInd] = expandName(eInd, mutableListOf(), ::uriParamsAmbiguitySolver)
            inds.remove(eInd)
        }
    }

    private fun uriParamsAmbiguitySolver(action: Action): List<String> {
        val restAction = action as RestCallAction
        val result = mutableListOf<String>()
        result.add(getPath(restAction.path.nameQualifier))

        val uriParams = restAction.parameters.filterIsInstance<PathParam>()
        result.add(with)
        val withParams = StringBuilder(uriParam)
        if (uriParams.size > 1) withParams.append("s")

        uriParams.forEach { param -> withParams.append("_${safeVariableName(param.primaryGene().getValueAsRawString())}") }

        result.add(withParams.append("_").toString())
        return result
    }

    private fun checkForQueryParams(individualToName: MutableMap<EvaluatedIndividual<*>, String>, inds: MutableSet<EvaluatedIndividual<*>>) {
        val groupByQueryParams = inds.groupBy {
            (it.evaluatedMainActions().last().action as RestCallAction).parameters.filterIsInstance<QueryParam>()
        }.filter { it.value.size == 1 && it.key.isNotEmpty()}

        groupByQueryParams.forEach { entry ->
            val eInd = entry.value[0]
            individualToName[eInd] = expandName(eInd, mutableListOf(), ::queryParamsAmbiguitySolver)
            inds.remove(eInd)
        }
    }

    private fun queryParamsAmbiguitySolver(action: Action): List<String> {
        val restAction = action as RestCallAction
        val result = mutableListOf<String>()
        result.add(getPath(restAction.path.nameQualifier))

        val queryParams = restAction.parameters.filterIsInstance<QueryParam>()
        result.add(with)
        val withParams = StringBuilder(queryParam)
        if (queryParams.size > 1) withParams.append("s")
        /*
          For better readability, multiple faults will be concatenated in a string separated by underscore
          to help understand it is a list of codes. Regardless of the outputFormat and language conventions.
         */
        queryParams.forEach { param -> withParams.append("_${safeVariableName(param.primaryGene().getValueAsRawString())}") }
        result.add(withParams.append("_").toString())
        return result
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
