package org.evomaster.core.output.naming.rest

import org.evomaster.core.output.naming.AmbiguitySolver
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.param.QueryParam
import org.evomaster.core.search.action.Action
import org.evomaster.core.search.gene.BooleanGene
import org.evomaster.core.search.gene.numeric.NumberGene
import org.evomaster.core.search.gene.string.StringGene

class QueryParamsAmbiguitySolver(private val nameWithQueryParameters: Boolean) : AmbiguitySolver {

    private val with = "with"
    private val param = "Param"
    private val queryParam = "query$param"
    private val and = "and"
    private val negative = "negative"
    private val empty = "empty"

    /*
     * If there are more than one query parameters, then use plural.
     */
    override fun apply(action: Action, remainingNameChars: Int): List<String> {
        val restAction = action as RestCallAction
        val result = mutableListOf<String>()

        val queryParams = restAction.path.getOnlyUsableQueries(restAction.parameters)
            .filter { it.getGeneForQuery().staticCheckIfImpactPhenotype() }
        val withTokens = listOf(with, if (queryParams.size > 1) "${queryParam}s" else queryParam)

        if (canAddNameTokens(withTokens, remainingNameChars)) {
            result.addAll(withTokens)
            if (nameWithQueryParameters) {
                val localCharsBudget = remainingNameChars - withTokens.sumOf { it.length }
                addQueryParameterNames(queryParams, result, localCharsBudget)
            }
        }
        return result
    }

    private fun addQueryParameterNames(queryParams: List<QueryParam>, result: MutableList<String>, remainingNameChars: Int) {
        var localCharsBudget = addBooleanQueryParams(queryParams, result, remainingNameChars)
        localCharsBudget = addNegativeNumbersQueryParams(queryParams, result, localCharsBudget)
        addEmptyStringQueryParams(queryParams, result, localCharsBudget)
    }

    private fun addBooleanQueryParams(queryParams: List<QueryParam>, result: MutableList<String>, remainingNameChars: Int): Int {
        val booleanQueryParams = getBooleanQueryParams(queryParams)
        var localCharsBudget = remainingNameChars
        booleanQueryParams.forEachIndexed { index, queryParam ->
            val localTokens = mutableListOf<String>()
            if (index != 0) {
                localTokens.add(and)
            }
            localTokens.add(queryParam.name)
            localCharsBudget = addNameTokensIfAllowed(result, localTokens, localCharsBudget)
        }
        return localCharsBudget
    }

    private fun addNegativeNumbersQueryParams(queryParams: List<QueryParam>, result: MutableList<String>, remainingNameChars: Int): Int {
        val numberQueryParams = getNegativeNumberQueryParams(queryParams)
        var localCharsBudget = remainingNameChars
        numberQueryParams.forEachIndexed { index, queryParam ->
            val localTokens = mutableListOf<String>()
            if (index != 0) {
                localTokens.add(and)
            }
            localTokens.add(negative)
            localTokens.add(queryParam.name)
            localCharsBudget = addNameTokensIfAllowed(result, localTokens, localCharsBudget)
        }
        return localCharsBudget
    }

    private fun addEmptyStringQueryParams(queryParams: List<QueryParam>, result: MutableList<String>, remainingNameChars: Int) {
        val emptyStringQueryParams = getEmptyStringQueryParams(queryParams)
        var localCharsBudget = remainingNameChars
        emptyStringQueryParams.forEachIndexed { index, queryParam ->
            val localTokens = mutableListOf<String>()
            if (index != 0) {
                localTokens.add(and)
            }
            localTokens.add(empty)
            localTokens.add(queryParam.name)
            localCharsBudget = addNameTokensIfAllowed(result, localTokens, localCharsBudget)
        }
    }

    private fun getBooleanQueryParams(queryParams: List<QueryParam>): List<QueryParam> {
        return queryParams.filter {
            val booleanGene = it.getGeneForQuery().getWrappedGene(BooleanGene::class.java)
            booleanGene != null && booleanGene.staticCheckIfImpactPhenotype() && booleanGene.value
        }
    }

    private fun getNegativeNumberQueryParams(queryParams: List<QueryParam>): List<QueryParam> {
        return queryParams.filter {
            val numberGene = it.getGeneForQuery().getWrappedGene(NumberGene::class.java)
            numberGene != null && numberGene.staticCheckIfImpactPhenotype() && numberGene.value.toLong() < 0
        }
    }

    private fun getEmptyStringQueryParams(queryParams: List<QueryParam>): List<QueryParam> {
        return queryParams.filter {
            val stringGene = it.getGeneForQuery().getWrappedGene(StringGene::class.java)
            stringGene != null && stringGene.staticCheckIfImpactPhenotype() && stringGene.getValueAsRawString().trim().isEmpty()
        }
    }

    private fun addNameTokensIfAllowed(nameTokens: MutableList<String>, targetStrings: List<String>, remainingNameChars: Int): Int {
        val charsToBeUsed = targetStrings.sumOf { it.length }
        if ((remainingNameChars - charsToBeUsed) >= 0) {
            nameTokens.addAll(targetStrings)
            return remainingNameChars - charsToBeUsed
        }
        return remainingNameChars
    }

    private fun canAddNameTokens(targetString: List<String>, remainingNameChars: Int): Boolean {
        return (remainingNameChars - targetString.sumOf { it.length }) >= 0
    }
}
