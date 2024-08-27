package org.evomaster.core.problem.rest.param

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.problem.api.param.Param
import org.evomaster.core.problem.util.HttpWsUtil.isContentTypeForm
import org.evomaster.core.problem.util.HttpWsUtil.isContentTypeJson
import org.evomaster.core.problem.util.HttpWsUtil.isContentTypeMultipartForm
import org.evomaster.core.problem.util.HttpWsUtil.isContentTypeTextPlain
import org.evomaster.core.problem.util.HttpWsUtil.isContentTypeXml
import org.evomaster.core.search.gene.BooleanGene
import org.evomaster.core.search.gene.collection.EnumGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.optional.CustomMutationRateGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory


class BodyParam(gene: Gene,
                typeGene: EnumGene<String>,
                /* this gene is meant to allow the BodyParam to present incorrect (i.e. unquoted)
                  string bodies.
                 */
                removeQuotesGene: CustomMutationRateGene<BooleanGene>? = null
) : Param("body", gene) {

    companion object {
        val log: Logger = LoggerFactory.getLogger(BodyParam::class.java)
    }

    val contenTypeGene : EnumGene<String>

    val notSupportedContentTypes : List<String>

    val contentRemoveQuotesGene: CustomMutationRateGene<BooleanGene>

    init {

        notSupportedContentTypes = typeGene.values.filter { !isSupportedType(it)}

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

        contentRemoveQuotesGene = removeQuotesGene
            ?: if (isJson() && primaryGene() is StringGene) {
                CustomMutationRateGene(
                    "sendUnquoteJsonStringWrapper",
                    BooleanGene("sendUnquoteJsonString", false), 0.25
                )
            } else {
                CustomMutationRateGene(
                    "sendUnquoteJsonStringWrapper",
                    BooleanGene("sendUnquoteJsonString", false), 0.0
                )
            }

        addChild(contentRemoveQuotesGene)

    }

    override fun copyContent(): Param {
        return BodyParam(primaryGene().copy(), contenTypeGene.copy() as EnumGene<String>, contentRemoveQuotesGene.copy() as CustomMutationRateGene<BooleanGene>)
    }

    override fun seeGenes() = listOf(primaryGene(), contenTypeGene, contentRemoveQuotesGene)

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

    private fun removeQuotes() = contentRemoveQuotesGene.gene.value && isJson()

    fun getValueAsPrintableString(mode: GeneUtils.EscapeMode? = null, targetFormat: OutputFormat? =null): String {
        val originalValueAsPrintableString =
            primaryGene().getValueAsPrintableString( mode= mode, targetFormat= targetFormat )
        val valueAsPrintableString = if (removeQuotes()) {
            GeneUtils.removeEnclosedQuotationMarks(originalValueAsPrintableString)
        } else {
            originalValueAsPrintableString
        }
        return valueAsPrintableString
    }
}
