package org.evomaster.core.problem.rest.param

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.problem.api.param.Param
import org.evomaster.core.problem.util.HttpWsUtil.isContentTypeForm
import org.evomaster.core.problem.util.HttpWsUtil.isContentTypeJson
import org.evomaster.core.problem.util.HttpWsUtil.isContentTypeMultipartForm
import org.evomaster.core.problem.util.HttpWsUtil.isContentTypeTextPlain
import org.evomaster.core.problem.util.HttpWsUtil.isContentTypeXml
import org.evomaster.core.search.gene.BooleanGene
import org.evomaster.core.search.gene.collection.ArrayGene
import org.evomaster.core.search.gene.collection.EnumGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.placeholder.CycleObjectGene
import org.evomaster.core.search.gene.wrapper.CustomMutationRateGene
import org.evomaster.core.search.gene.wrapper.OptionalGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory


/**
 * Info of a part of a multipart/form-data body, taken from the OpenAPI schema.
 *
 * @param isFile whether the part needs a "filename" in its Content-Disposition
 * @param contentType from `encoding[<property>].contentType`, if declared
 */
data class PartMeta(val isFile: Boolean, val contentType: String? = null)


class BodyParam(gene: Gene,
                typeGene: EnumGene<String>,
                /* this gene is meant to allow the BodyParam to present incorrect (i.e. unquoted)
                  string bodies.
                 */
                removeQuotesGene: CustomMutationRateGene<BooleanGene>? = null,
                /**
                 * Info of each part of a multipart/form-data body, by property name.
                 * Empty if multipart support is disabled.
                 */
                val parts: Map<String, PartMeta> = mapOf()
) : Param("body", gene) {

    companion object {
        val log: Logger = LoggerFactory.getLogger(BodyParam::class.java)

        //fixed, to keep generated tests stable
        const val DEFAULT_BOUNDARY = "EvoMasterBoundary7f3a"

        private const val CRLF = "\r\n"

        //many endpoints check the file extension before reading the content
        fun extensionForContentType(contentType: String): String = when {
            contentType.contains("image/png", true) -> ".png"
            contentType.contains("image/jpeg", true) -> ".jpg"
            contentType.contains("image/gif", true) -> ".gif"
            contentType.contains("image/svg", true) -> ".svg"
            contentType.contains("application/pdf", true) -> ".pdf"
            contentType.contains("text/csv", true) -> ".csv"
            contentType.contains("zip", true) -> ".zip"
            contentType.contains("json", true) -> ".json"
            contentType.contains("xml", true) -> ".xml"
            contentType.contains("text/plain", true) -> ".txt"
            else -> ".bin"
        }
    }

    constructor(genes: List<Gene>, parts: Map<String, PartMeta> = mapOf())
            : this(genes[0], genes[1] as EnumGene<String>, genes[2] as CustomMutationRateGene<BooleanGene>?, parts){
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

        val options = typeGene.values
            //.filter { isSupportedType(it) } //not supported will be treated as TEXT, to avoid useless 415 requests
            .filter {it != "*/*"} //this makes no sense as content-type, but it looks like might happen when wrong V2
                                  //is converted to V3
            .toMutableList()


        if(typeGene.values.any { isContentTypeMultipartForm(it) }){
                /*
                    This is tricky... we have seen cases in V2 in which formData without an explicit
                    application/x-www-form-urlencoded  turns by V3 parser into a multipart/form-data.
                    This was a major issue that happened for features-service when updating parser
                    from V2 to V3.
                    So, urlencoded is added as an extra option, and the search decides which one works.
                 */
                options.add("application/x-www-form-urlencoded")
        }

        if(options.isEmpty()){
            /*
                 If no info, we just try with JSON
             */
            options.add("application/json")
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
        return BodyParam(primaryGene().copy(), contentTypeGene.copy() as EnumGene<String>, contentRemoveQuotesGene.copy() as CustomMutationRateGene<BooleanGene>, parts)
    }

    override fun seeGenes() = listOf(primaryGene(), contentTypeGene, contentRemoveQuotesGene)

    /**
     * Selected media type, without parameters. For the header value, see [contentTypeHeader]
     */
    fun contentType() = contentTypeGene.getValueAsRawString().trim()

    /**
     * Value of the Content-Type header, ie, including the boundary for multipart
     */
    fun contentTypeHeader(): String =
        if (isMultipartSupported()) "${contentType()}; boundary=${multipartBoundary()}"
        else contentType()

    //multipart is supported only if [parts] is not empty
    private fun isSupportedType(s: String) = isContentTypeJson(s) || isContentTypeXml(s) || isContentTypeTextPlain(s)
            || isContentTypeForm(s) || (isContentTypeMultipartForm(s) && parts.isNotEmpty())

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

    fun isMultipartSupported() = isMultipartForm() && parts.isNotEmpty()

    /**
     * [DEFAULT_BOUNDARY], with a numeric suffix if it appears inside a part value
     */
    fun multipartBoundary(): String {
        val values = multipartParts().map { it.leaf.getValueAsRawString() }
        var boundary = DEFAULT_BOUNDARY
        var i = 2
        while (values.any { it.contains(boundary) }) {
            boundary = DEFAULT_BOUNDARY + (i++)
        }
        return boundary
    }

    /**
     * @param repeatedIndex position of the element in an array property, null otherwise
     */
    private class MultipartPart(val name: String, val leaf: Gene, val repeatedIndex: Int?)

    /**
     * Parts to send, in schema order. An array property is sent as one part per element, all with the same name.
     */
    private fun multipartParts(): List<MultipartPart> {
        val obj = primaryGene().getWrappedGene(ObjectGene::class.java) ?: return listOf()
        return obj.fixedFields
            .filter { isPartToBePrinted(it) }
            .flatMap { field ->
                val leaf = field.getLeafGene()
                if (leaf is ArrayGene<*>) {
                    leaf.getViewOfElements()
                        .filter { isPartToBePrinted(it) }
                        .mapIndexed { i, e -> MultipartPart(field.name, e.getLeafGene(), i) }
                } else {
                    listOf(MultipartPart(field.name, leaf, null))
                }
            }
    }

    private fun isPartToBePrinted(g: Gene): Boolean =
        g !is CycleObjectGene
                && (g !is OptionalGene || (g.isActive && g.gene !is CycleObjectGene))
                && g.isPrintable()

    /**
     * Declared `encoding` content type, else the OpenAPI default
     */
    private fun partContentType(name: String, leaf: Gene): String {
        val meta = parts[name]
        return meta?.contentType
            ?: when {
                meta?.isFile == true -> "application/octet-stream"
                leaf is ObjectGene -> "application/json"
                else -> "text/plain"
            }
    }

    /**
     * For array elements, the index is added to keep filenames distinct
     */
    private fun partFilename(part: MultipartPart, partContentType: String): String {
        val suffix = if (part.repeatedIndex == null) "" else "_${part.repeatedIndex}"
        return "${part.name}$suffix${extensionForContentType(partContentType)}"
    }

    private fun renderMultipartBody(targetFormat: OutputFormat?): String {

        val boundary = multipartBoundary()
        val buffer = StringBuilder()

        multipartParts().forEach { part ->

            val partType = partContentType(part.name, part.leaf)

            val value = if (isContentTypeJson(partType)) {
                part.leaf.getValueAsPrintableString(mode = GeneUtils.EscapeMode.JSON, targetFormat = targetFormat)
            } else {
                //raw value, unquoted
                part.leaf.getValueAsRawString()
            }

            buffer.append("--").append(boundary).append(CRLF)
            buffer.append("Content-Disposition: form-data; name=\"").append(part.name).append("\"")
            if (parts[part.name]?.isFile == true) {
                buffer.append("; filename=\"").append(partFilename(part, partType)).append("\"")
            }
            buffer.append(CRLF)
            buffer.append("Content-Type: ").append(partType).append(CRLF)
            buffer.append(CRLF)
            buffer.append(value).append(CRLF)
        }

        buffer.append("--").append(boundary).append("--").append(CRLF)

        return buffer.toString()
    }

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

        //built here and not in the gene, as it needs [parts]
        if (mode == GeneUtils.EscapeMode.MULTIPART_FORM_DATA) {
            return if (isMultipartSupported()) {
                renderMultipartBody(targetFormat)
            } else {
                primaryGene().getValueAsPrintableString(mode = GeneUtils.EscapeMode.TEXT, targetFormat = targetFormat)
            }
        }

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
