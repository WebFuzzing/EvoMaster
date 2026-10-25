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
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.placeholder.CycleObjectGene
import org.evomaster.core.search.gene.wrapper.CustomMutationRateGene
import org.evomaster.core.search.gene.wrapper.OptionalGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory


/**
 * Metadata of a single part of a multipart/form-data body, computed from the OpenAPI schema at
 * build time. It is not a gene: it cannot be derived from the gene tree, since a [StringGene]
 * carries no marker distinguishing a text field from a file.
 *
 * @param isFile whether the part must carry a "filename" parameter in its Content-Disposition.
 *               Server frameworks use its presence to decide whether a part is a file upload at
 *               all (e.g. in Spring, a part without a filename is not bound to a MultipartFile).
 * @param contentType the Content-Type of the part, taken from `encoding[<property>].contentType`
 *                    when the schema declares it, else null to fall back to the OpenAPI default.
 */
data class PartMeta(val isFile: Boolean, val contentType: String? = null)


class BodyParam(gene: Gene,
                typeGene: EnumGene<String>,
                /* this gene is meant to allow the BodyParam to present incorrect (i.e. unquoted)
                  string bodies.
                 */
                removeQuotesGene: CustomMutationRateGene<BooleanGene>? = null,
                /**
                 * Per-part metadata for a multipart/form-data body, keyed by property name.
                 *
                 * Empty for every other content type, and also for multipart when the experimental
                 * support is disabled: as such, a non-empty map is what enables the multipart
                 * serialization, see [isSupportedType] and [getValueAsPrintableString].
                 *
                 * NOTE: this is not a gene, so it is invisible to the search; but it must be
                 * propagated in [copyContent], as individuals are copied constantly.
                 */
                val parts: Map<String, PartMeta> = mapOf()
) : Param("body", gene) {

    companion object {
        val log: Logger = LoggerFactory.getLogger(BodyParam::class.java)

        /**
         * The boundary is chosen by the client, never by the schema: OpenAPI has no place for it, and
         * the server reads it from the Content-Type header. A constant keeps generated tests stable
         * and easy to diff. See [multipartBoundary] for the collision case.
         */
        const val DEFAULT_BOUNDARY = "EvoMasterBoundary7f3a"

        private const val CRLF = "\r\n"

        /**
         * Extension appended to the filename of a file part, derived from the part's content type.
         * Many endpoints filter on the extension before looking at a single byte of the content, so a
         * filename without one (or with a meaningless one) makes the upload code path unreachable.
         */
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
                    We currently do not support multipart/form-data properly.
                    If we see it, we just treat it as a regular form for now
                    TODO handle it properly
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
     * Which content type the search has selected. A pure function of [contentTypeGene], with no
     * parameters, and the value queried by [isJson], [isXml], [isForm], [isMultipartForm] and by the
     * HTTP semantics oracle. For what actually goes in the Content-Type header, see [contentTypeHeader].
     */
    fun contentType() = contentTypeGene.getValueAsRawString().trim()

    /**
     * The full value of the Content-Type header, i.e. the media type plus its parameters. For
     * multipart that means the mandatory "boundary", without which the server cannot split the body
     * into parts at all.
     *
     * Kept separate from [contentType] on purpose: multipart is the first content type in EvoMaster
     * that needs a parameter, and folding it into [contentType] would make a method that reads one
     * gene start depending on the value of every other gene in the body.
     */
    fun contentTypeHeader(): String =
        if (isMultipartSupported()) "${contentType()}; boundary=${multipartBoundary()}"
        else contentType()

    /*
        multipart is reported as supported only once [parts] has been populated, which the builder does
        only when the experimental support is enabled. With the flag off, multipart keeps being listed in
        [notSupportedContentTypes] and treated as TEXT, exactly as before this feature.
     */
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

    /**
     * True when the selected content type is multipart AND the per-part metadata is available, i.e.
     * when this body can actually be serialized as a valid multipart envelope.
     */
    fun isMultipartSupported() = isMultipartForm() && parts.isNotEmpty()

    /**
     * The boundary effectively used, i.e. [DEFAULT_BOUNDARY] unless it happens to occur inside the
     * value of a part, in which case an incremental suffix is appended. A random [StringGene] will
     * essentially never produce that sequence by chance, but a collision would break the request
     * silently, and the check is cheap.
     *
     * A pure function of the current gene values, so the header and the envelope agree as long as no
     * mutation happens between the two calls.
     */
    fun multipartBoundary(): String {
        val values = multipartParts().map { it.second.getValueAsRawString() }
        var boundary = DEFAULT_BOUNDARY
        var i = 2
        while (values.any { it.contains(boundary) }) {
            boundary = DEFAULT_BOUNDARY + (i++)
        }
        return boundary
    }

    /**
     * The parts to be sent, in schema order, as (property name, leaf gene) pairs. Inactive
     * [OptionalGene]s are skipped, so a non-required property simply has no part.
     *
     * Same inclusion rules as [ObjectGene] uses for its own serializations.
     */
    private fun multipartParts(): List<Pair<String, Gene>> {
        val obj = primaryGene().getWrappedGene(ObjectGene::class.java) ?: return listOf()
        return obj.fixedFields
            .filter { it !is CycleObjectGene && (it !is OptionalGene || (it.isActive && it.gene !is CycleObjectGene)) }
            .filter { it.isPrintable() }
            .map { it.name to it.getLeafGene() }
    }

    /**
     * Content-Type of a single part: the value declared in `encoding[<property>].contentType` when
     * present, else the default that the OpenAPI specification prescribes for the property's schema.
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
     * The RFC 7578 envelope. Note that no part carries real binary content — every value comes out of
     * a gene as text — so the body is a String and there is nothing that UTF-8 encoding could corrupt.
     */
    private fun renderMultipartBody(targetFormat: OutputFormat?): String {

        val boundary = multipartBoundary()
        val buffer = StringBuilder()

        multipartParts().forEach { (name, leaf) ->

            val partType = partContentType(name, leaf)

            val value = if (isContentTypeJson(partType)) {
                leaf.getValueAsPrintableString(mode = GeneUtils.EscapeMode.JSON, targetFormat = targetFormat)
            } else {
                //same as the x-www-form-urlencoded serialization: the raw value, unquoted
                leaf.getValueAsRawString()
            }

            buffer.append("--").append(boundary).append(CRLF)
            buffer.append("Content-Disposition: form-data; name=\"").append(name).append("\"")
            if (parts[name]?.isFile == true) {
                buffer.append("; filename=\"").append(name).append(extensionForContentType(partType)).append("\"")
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

        /*
            The multipart envelope is assembled here, and not in the gene, because this is the only
            object that has both the gene tree and the per-part metadata. ObjectGene is generic and has
            no business knowing what multipart is.

            No gene implements MULTIPART_FORM_DATA, so that mode must never be forwarded: a gene would
            throw on it. If it arrives when multipart is not supported (e.g. the content-type gene has
            meanwhile mutated to another option), the payload degrades to TEXT, as it did before this
            feature.
         */
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
