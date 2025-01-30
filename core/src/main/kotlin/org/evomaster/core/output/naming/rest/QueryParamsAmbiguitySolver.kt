package org.evomaster.core.output.naming.rest

import org.evomaster.core.output.naming.AmbiguitySolver
import org.evomaster.core.problem.rest.RestCallAction
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
    override fun apply(action: Action): List<String> {
        val restAction = action as RestCallAction
        val result = mutableListOf<String>()

        val queryParams = restAction.path.getOnlyUsableQueries(restAction.parameters)
            .filter { it.getGeneForQuery().staticCheckIfImpactPhenotype() }
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
            result.add(negative)
            result.add(queryParam.name)
            if (index != numberQueryParams.lastIndex) {
                result.add(and)
            }
        }

        val emptyStringQueryParams = getEmptyStringQueryParams(queryParams)
        emptyStringQueryParams.forEachIndexed { index, queryParam ->
            result.add(empty)
            result.add(queryParam.name)
            if (index != emptyStringQueryParams.lastIndex) {
                result.add(and)
            }
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
}
