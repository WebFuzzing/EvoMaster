package org.evomaster.core.problem.rest.param

import org.evomaster.core.search.gene.EnumGene
import org.evomaster.core.search.gene.Gene


class BodyParam(gene: Gene,
                val contenTypeGene: EnumGene<String>
) : Param("body", gene){

    override fun copy(): Param {
        return BodyParam(gene.copy(), contenTypeGene.copy() as EnumGene<String>)
    }

    override fun seeGenes() = listOf(gene, contenTypeGene)

    fun contentType() = contenTypeGene.getValueAsRawString().trim()

    /*
        FIXME these check could be more precise.
        Need to consider ";" and "vnd" formats, where ending defines the
        type with a "+"
     */

    fun isJson() = contentType().contains("json", ignoreCase = true)

    fun isXml() = contentType().contains("xml", ignoreCase = true)

    fun isTextPlain() = contentType().contains("text/plain", ignoreCase = true)

    fun isForm() = contentType().contains("application/x-www-form-urlencoded", ignoreCase = true)
}