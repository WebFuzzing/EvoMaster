package org.evomaster.core.problem.rest.param

import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.problem.api.param.Param
import org.evomaster.core.problem.util.HttpWsUtil
import org.evomaster.core.problem.util.HttpWsUtil.isContentTypeForm
import org.evomaster.core.problem.util.HttpWsUtil.isContentTypeJson
import org.evomaster.core.problem.util.HttpWsUtil.isContentTypeMultipartForm
import org.evomaster.core.problem.util.HttpWsUtil.isContentTypeTextPlain
import org.evomaster.core.problem.util.HttpWsUtil.isContentTypeXml
import org.evomaster.core.search.gene.collection.EnumGene
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

            if(typeGene.values.any { isContentTypeMultipartForm(it) }){
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
        if(typeGene.initialized) contenTypeGene.markAllAsInitialized()
        // local id of typeGene needs to be assigned for the newly created contenTypeGene since it is from copy
        if (typeGene.hasLocalId()) contenTypeGene.setLocalId(typeGene.getLocalId())
        addChild(contenTypeGene)
    }


    override fun copyContent(): Param {
        return BodyParam(gene.copy(), contenTypeGene.copy() as EnumGene<String>)
    }

    override fun seeGenes() = listOf(gene, contenTypeGene)

    fun contentType() = contenTypeGene.getValueAsRawString().trim()

    private fun isSupportedType(s: String) = isContentTypeJson(s) || isContentTypeXml(s) || isContentTypeTextPlain(s) || isContentTypeForm(s)

    /*
        FIXME these check could be more precise.
        Need to consider ";" and "vnd" formats, where ending defines the
        type with a "+"
     */

    fun isJson() = isContentTypeJson(contentType())

    fun isXml() = isContentTypeXml(contentType())

    fun isTextPlain() = isContentTypeTextPlain(contentType())

    fun isForm() = isContentTypeForm(contentType())

    fun isMultipartForm() = isContentTypeMultipartForm(contentType())
}