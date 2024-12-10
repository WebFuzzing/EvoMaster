package org.evomaster.core.problem.externalservice.param

import org.evomaster.core.problem.api.param.Param
import org.evomaster.core.search.gene.collection.EnumGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.optional.OptionalGene

/**
 * represent external service responses which contains
 * @property responseType the data type of the response
 * @property responseBody represents the response to return
 * @property extraProperties represent the extra properties to describe the responses, such as status code for HTTP response,
 * note that all [extraProperties] are considered as part of children of this param.
 */
abstract class ResponseParam(
        name: String,
        val responseType: EnumGene<String>,
        val responseBody: OptionalGene,
        val extraProperties: List<Gene>
) : Param(name, listOf(responseBody).plus(extraProperties).plus(responseType).toMutableList()){

    fun isJson() = responseType.getValueAsRawString().contains("json", ignoreCase = true)

}