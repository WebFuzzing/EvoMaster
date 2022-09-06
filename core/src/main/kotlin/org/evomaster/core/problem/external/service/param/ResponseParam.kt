package org.evomaster.core.problem.external.service.param

import org.evomaster.core.problem.api.service.param.Param
import org.evomaster.core.search.gene.EnumGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.OptionalGene

/**
 * represent external service responses which contains
 * @property responseType the data type of the response
 * @property response represents the response to return
 * @property extraProperties represent the extra properties to describe the responses, such as status code for HTTP response,
 * note that all [extraProperties] are considered as part of children of this param.
 */
abstract class ResponseParam(
    name: String,
    val responseType: EnumGene<String>,
    val response: OptionalGene,
    val extraProperties: List<Gene>
) : Param(name, extraProperties.plus(response).toMutableList())