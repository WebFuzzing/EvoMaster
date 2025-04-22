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

    constructor(genes: List<Gene>) : this(genes[0], genes[1] as EnumGene<String>, genes[2] as CustomMutationRateGene<BooleanGene>?){
        if(genes.size != 3){
            throw IllegalArgumentException("Must get 3 genes as input, got ${genes.size}")
        }
    }

    val contentTypeGene : EnumGene<String>

    val notSupportedContentTypes : List<String>

    /**
     * This is to handle a very common bug in few Java REST frameworks.
     * They might declare to take as input a JSON string when OpenAPI schema is automatically
     * generated, but then they actually handle the body payload as a TEXT
     */
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

        contentTypeGene = EnumGene(typeGene.name, options, typeGene.index)
        if(typeGene.initialized) contentTypeGene.markAllAsInitialized()
        // local id of typeGene needs to be assigned for the newly created contenTypeGene since it is from copy
        if (typeGene.hasLocalId()) contentTypeGene.setLocalId(typeGene.getLocalId())
        addChild(contentTypeGene)

        contentRemoveQuotesGene = removeQuotesGene
            ?: CustomMutationRateGene(
                    "sendUnquoteJsonStringWrapper",
                    //this should be set once randomized, but then not changed during search via mutation
                    BooleanGene("sendUnquoteJsonString", false), 0.0
                )
        addChild(contentRemoveQuotesGene)
    }

    fun isJsonString() : Boolean{
        return isJson() && primaryGene().getWrappedGene(StringGene::class.java) != null
    }

    override fun copyContent(): Param {
        return BodyParam(primaryGene().copy(), contentTypeGene.copy() as EnumGene<String>, contentRemoveQuotesGene.copy() as CustomMutationRateGene<BooleanGene>)
    }

    override fun seeGenes() = listOf(primaryGene(), contentTypeGene, contentRemoveQuotesGene)

    fun contentType() = contentTypeGene.getValueAsRawString().trim()

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

    fun shouldRemoveQuotesInJsonString() = contentRemoveQuotesGene.gene.value && isJsonString()

    fun isStringUsedAsText(mode: GeneUtils.EscapeMode?): Boolean{
        return mode == GeneUtils.EscapeMode.TEXT && primaryGene().getWrappedGene(StringGene::class.java) != null
    }

    fun getRawStringToBeSent(mode: GeneUtils.EscapeMode? = null, targetFormat: OutputFormat? =null) : String{
        val s = getValueAsPrintableString(mode = mode, targetFormat = targetFormat)
        //FIXME
//        if(isStringUsedAsText(mode)){
//            return GeneUtils.removeEnclosedQuotationMarks(s)
//        }
        return s
    }

    fun getValueAsPrintableString(mode: GeneUtils.EscapeMode? = null, targetFormat: OutputFormat? =null): String {

        val originalValueAsPrintableString =
            primaryGene().getValueAsPrintableString( mode= mode, targetFormat= targetFormat )


        if(shouldRemoveQuotesInJsonString()){ // || isStringUsedAsText(mode)){
            org.evomaster.core.Lazy.assert{originalValueAsPrintableString.startsWith("\"")
                    && originalValueAsPrintableString.endsWith("\"")
                    && originalValueAsPrintableString.length >= 2
            }
            return GeneUtils.removeEnclosedQuotationMarks(originalValueAsPrintableString)
        }

        return originalValueAsPrintableString
    }
}
