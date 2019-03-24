package org.evomaster.experiments.objects.param

import org.evomaster.core.search.gene.EnumGene
import org.evomaster.core.search.gene.Gene


class BodyParam(gene: Gene,
                val contenTypeGene: EnumGene<String>
) : Param("body", gene){

    override fun copy(): Param {
        return BodyParam(gene.copy(), contenTypeGene.copy() as EnumGene<String>)
    }

    override fun seeGenes() = listOf(gene, contenTypeGene)

    fun contentType() = contenTypeGene.getValueAsRawString()

    //FIXME these check could be more precise

    fun isJson() = contentType().contains("json", ignoreCase = true)

    fun isXml() = contentType().contains("xml", ignoreCase = true)

    fun isTextPlain() = contentType().contains("text/plain", ignoreCase = true)

}