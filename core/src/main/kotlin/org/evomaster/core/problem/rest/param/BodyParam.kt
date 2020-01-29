package org.evomaster.core.problem.rest.param

import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.search.gene.EnumGene
import org.evomaster.core.search.gene.Gene
import org.slf4j.Logger
import org.slf4j.LoggerFactory


class BodyParam(gene: Gene,
                typeGene: EnumGene<String>
) : Param("body", gene) {

    companion object {
        val log: Logger = LoggerFactory.getLogger(BodyParam::class.java)
    }

    val contenTypeGene : EnumGene<String>

    init {
        typeGene.values.forEach {
            if (!isSupportedType(it)) {
                LoggingUtil.uniqueWarn(log, "Not supported data type: $it")
            }
        }

        val options = typeGene.values.filter { isSupportedType(it) }.toMutableList()
        if(options.isEmpty()){
            /*
                If no info, or not supported, we just try with JSON
             */
            options.add("application/json")
        }

       contenTypeGene = EnumGene(typeGene.name, options, typeGene.index)
    }

    override fun copy(): Param {
        return BodyParam(gene.copy(), contenTypeGene.copy() as EnumGene<String>)
    }

    override fun seeGenes() = listOf(gene, contenTypeGene)

    fun contentType() = contenTypeGene.getValueAsRawString().trim()

    private fun isSupportedType(s: String) = isJson(s) || isXml(s) || isTextPlain(s) || isForm(s)

    /*
        FIXME these check could be more precise.
        Need to consider ";" and "vnd" formats, where ending defines the
        type with a "+"
     */

    private fun isJson(s: String) = s.contains("json", ignoreCase = true)

    fun isJson() = isJson(contentType())

    private fun isXml(s: String) = s.contains("xml", ignoreCase = true)

    fun isXml() = isXml(contentType())

    private fun isTextPlain(s: String) = s.contains("text/plain", ignoreCase = true)

    fun isTextPlain() = isTextPlain(contentType())

    private fun isForm(s: String) = s.contains("application/x-www-form-urlencoded", ignoreCase = true)

    fun isForm() = isForm(contentType())
}