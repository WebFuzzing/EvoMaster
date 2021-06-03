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

            if(typeGene.values.any { isMultipartForm(it) }){
                /*
                    This is tricky... we have seen cases in V2 in which formData without an explicit
                    application/x-www-form-urlencoded  turns by V3 parser into a multipart/form-data.
                    This was a major issue that happened for features-service when updating parser
                    from V2 to V3.
                    We currently do not support multipart/form-data properly.
                    If we see it, we just treat it as a regular form for now
                    TODO handle it properly
                 */
                options.add("application/x-www-form-urlencoded")

            } else {

                /*
                 If no info, or not supported, we just try with JSON
                */
                options.add("application/json")
            }
        }

        contenTypeGene = EnumGene(typeGene.name, options, typeGene.index)
        addChild(contenTypeGene)
    }

    override fun getChildren(): List<Gene> = listOf(gene, contenTypeGene)

    override fun copyContent(): Param {
        return BodyParam(gene.copyContent(), contenTypeGene.copyContent() as EnumGene<String>)
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

    private fun isMultipartForm(s: String ) = s.contains("multipart/form-data", ignoreCase = true)

    fun isMultipartForm() = isMultipartForm(contentType())
}