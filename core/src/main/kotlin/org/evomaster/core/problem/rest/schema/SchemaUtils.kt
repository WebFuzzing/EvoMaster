package org.evomaster.core.problem.rest.schema


import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.examples.Example
import io.swagger.v3.oas.models.links.Link
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.parameters.Parameter
import io.swagger.v3.oas.models.parameters.RequestBody
import io.swagger.v3.oas.models.responses.ApiResponse
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.problem.rest.StatusGroup
import org.evomaster.core.problem.rest.data.HttpVerb
import java.net.URI
import java.net.URISyntaxException
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.io.path.walk

/**
 * https://swagger.io/docs/specification/v3_0/using-ref/
 * https://swagger.io/docs/specification/v3_0/components/
 */
object SchemaUtils {

    private val log = org.slf4j.LoggerFactory.getLogger(SchemaUtils::class.java)


    /*
        For handling of $ref
        https://swagger.io/specification/   (Components Object)

        - schemas (DONE)
        - responses (DONE)
        - parameters (DONE)
        - examples (DONE)
        - requestBodies (DONE)
        - headers (TODO is it needed? this is for response objects)
        - securitySchemes (TODO is it needed?)
        - links (DONE)
        - callbacks (TODO is it needed?)
        - pathItems (DONE)
     */


    fun isLocalRef(sref: String) = sref.startsWith("#")

    private fun extractLocation(sref: String, messages: MutableList<String>) : String?{
        if(!sref.contains("#")){
            messages.add("Not a valid \$ref, as it contains no #: $sref")
            return null
        }
        return sref.substring(0, sref.indexOf("#"))
    }


    fun computeLocation(ref: String, currentSource: SchemaLocation, messages: MutableList<String>) : String?{

        val rawLocation = extractLocation(ref, messages)
            ?: return null

        if(rawLocation.startsWith("http:",true) || rawLocation.startsWith("https:",true)){
            //location is absolute, so no need to do anything
            return rawLocation
        }

        //TODO does it make any sense to have file:// here???

        if(currentSource.type == SchemaLocationType.MEMORY){
            throw IllegalArgumentException("Can't handle relative location for memory files: $rawLocation")
        }

        val csl = currentSource.location

        if(rawLocation.startsWith("//")){
            //as per specs, use same protocol as source
            val protocol = csl.substring(0, csl.indexOf(":"))
            if(protocol.isBlank()){
                log.warn("No protocol can be inferred for $rawLocation from $csl")
            }
            return "$protocol:$rawLocation"
        }

        //if arrive here, it is a relative path
        val delimiter = if(csl.endsWith("/")) "" else "/"
        val parentFolder = "../" // this is based to what discussed in the specs

        val location = "$csl$delimiter$parentFolder$rawLocation"

        //FIXME should not usi URI
        return try{
            URI(location).normalize().toString()
        } catch (e: Exception){
            location
        }
    }


    private fun extractReferenceName(reference: String, messages: MutableList<String>): String {
        try {
            /*
                FIXME!!! This is broken... Java URI should not be used. Add rather UriUtils from Spring
                https://dev.to/authlete/java-uri-library-compliant-with-rfc-3986-21bb
                https://bugs.openjdk.org/browse/JDK-8019345
                https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/util/UriUtils.html
             */
            URI(reference)
        } catch (e: URISyntaxException) {
            messages.add("Object reference is not a valid URI: $reference")
        }

        //token after last /
        return reference.substring(reference.lastIndexOf("/") + 1)
    }

    fun getReferenceLink(
        schema: RestSchema,
        current: SchemaOpenAPI,
        reference: String,
        messages: MutableList<String>
    ) : Link?{
        return getReference(schema,current,reference,messages,SchemaUtils::getLink)
    }

    private fun getLink(openAPI: SchemaOpenAPI, name: String, messages: MutableList<String>,reference: String) : Link?{
        val link = openAPI.schemaParsed.components?.links?.get(name)
        if (link == null) {
            messages.add("Cannot find reference to link $reference")
        }
        return link
    }

    private fun <T> getReference(schema: RestSchema,
                     current: SchemaOpenAPI,
                     reference: String,
                     messages: MutableList<String>,
                     lambda: (
                         current: SchemaOpenAPI,
                         name: String,
                         messages: MutableList<String>,
                         reference: String)
                     -> T?
    ) : T? {
        val name = extractReferenceName(reference,messages)
        if(isLocalRef(reference)){
            return lambda(current, name, messages, reference)
        } else {
            val location = computeLocation(reference, current.sourceLocation, messages)
                ?: return null
            val other = schema.getSpec(location)
            if(other == null){
                messages.add("Cannot retrieve schema from:  $location")
                return null
            }
            return  lambda(other, name, messages, reference)
        }
    }

    fun getReferenceParameter(
        schema: RestSchema,
        current: SchemaOpenAPI,
        reference: String,
        messages: MutableList<String>
    ) : Parameter?{
        return getReference(schema,current,reference,messages,SchemaUtils::getParameter)
    }

    private fun getParameter(openAPI: SchemaOpenAPI, name: String, messages: MutableList<String>,reference: String): Parameter?{
        val p = openAPI.schemaParsed.components?.parameters?.get(name)
        if(p==null){
            messages.add("Cannot find reference to parameter $reference")
        }
        return p
    }


    fun getReferencePathItem(
        schema: RestSchema,
        current: SchemaOpenAPI,
        reference: String,
        messages: MutableList<String>
    ) : PathItem?{
        return getReference(schema,current,reference,messages,SchemaUtils::getPathItem)
    }

    private fun getPathItem(openAPI: SchemaOpenAPI, name: String, messages: MutableList<String>,reference: String): PathItem?{
        val p = openAPI.schemaParsed.components?.pathItems?.get(name)
        if(p == null){
            messages.add("Cannot find reference to path item $reference")
        }
        return p
    }

    fun getReferenceExample(
        schema: RestSchema,
        current: SchemaOpenAPI,
        reference: String,
        messages: MutableList<String>
    ) : Example?{
        return getReference(schema,current,reference,messages,SchemaUtils::getExample)
    }

    private fun getExample(openAPI: SchemaOpenAPI, name: String, messages: MutableList<String>, reference: String) : Example?{
        val e = openAPI.schemaParsed.components?.examples?.get(name)
        if(e == null){
            messages.add("Cannot find reference to example $reference")
        }
        return e
    }


    fun getReferenceRequestBody(
        schema: RestSchema,
        current: SchemaOpenAPI,
        reference: String,
        messages: MutableList<String>
    ) : RequestBody?{
        return getReference(schema,current,reference,messages,SchemaUtils::getRequestBody)
    }

    private fun getRequestBody(openAPI: SchemaOpenAPI, name: String, messages: MutableList<String>,reference: String): RequestBody? {
        val body =  openAPI.schemaParsed.components?.requestBodies?.get(name)
        if(body == null){
            messages.add("Cannot find reference to request body $reference")
        }
        return body
    }


    fun getReferenceSchema(
        schema: RestSchema,
        current: SchemaOpenAPI,
        reference: String,
        messages: MutableList<String>
    ) : Schema<*>?{
        return getReference(schema,current,reference,messages,SchemaUtils::getSchema)
    }

    private fun getSchema(openAPI: SchemaOpenAPI, name: String, messages: MutableList<String>,reference: String): Schema<*>? {
        val schema =  openAPI.schemaParsed.components?.schemas?.get(name)
        if(schema == null){
            messages.add("Cannot find reference to schema object $reference")
        }
        return schema
    }


    fun getReferenceResponse(
        schema: RestSchema,
        current: SchemaOpenAPI,
        reference: String,
        messages: MutableList<String>
    ) : ApiResponse?{
        return getReference(schema,current,reference,messages,SchemaUtils::getResponse)
    }

    private fun getResponse(openAPI: SchemaOpenAPI, name: String, messages: MutableList<String>,reference: String): ApiResponse? {
        val response = openAPI.schemaParsed.components?.responses?.get(name)
        if(response == null){
            messages.add("Cannot find response object $reference")
        }
        return response
    }

    /**
     * Recursively collect property names from a schema, resolving `$ref` at every level
     * and merging the union of properties across `allOf` parts.
     *
     * `oneOf` / `anyOf` are intentionally ignored: their semantics are alternatives, not
     * a stable set of writable fields, so including them would produce false positives
     * in callers that compare PUT-sent fields to GET-returned ones.
     *
     * Cycle protection: a `$ref` already on the resolution stack is skipped, which is
     * safe for property-name collection (the names are union-merged into a Set).
     *
     * Looks at the original schema (not at the parsed [RestCallAction] gene tree) because
     * taint analysis can rewrite the action's gene tree, while the spec is the source of truth.
     */
    fun collectPropertyNames(
        schema: RestSchema,
        raw: Schema<*>?,
        visitedRefs: MutableSet<String> = mutableSetOf()
    ): Set<String> {
        if (raw == null) return emptySet()

        val ref = raw.`$ref`
        val resolved: Schema<*>? = if (ref != null) {
            if (!visitedRefs.add(ref)) return emptySet()
            getReferenceSchema(schema, schema.main, ref, mutableListOf())
        } else raw

        if (resolved == null) return emptySet()

        val result = mutableSetOf<String>()
        resolved.properties?.keys?.let { result.addAll(it) }
        resolved.allOf?.forEach { result.addAll(collectPropertyNames(schema, it, visitedRefs)) }
        return result
    }

    /**
     * Returns the operation associated with [verb] in the given [pathItem], or null if absent.
     */
    private fun pathItemOperation(pathItem: PathItem, verb: HttpVerb): Operation? = when (verb) {
        HttpVerb.GET     -> pathItem.get
        HttpVerb.POST    -> pathItem.post
        HttpVerb.PUT     -> pathItem.put
        HttpVerb.DELETE  -> pathItem.delete
        HttpVerb.OPTIONS -> pathItem.options
        HttpVerb.PATCH   -> pathItem.patch
        HttpVerb.HEAD    -> pathItem.head
        HttpVerb.TRACE   -> pathItem.trace
    }

    /**
     * Status-key matchers for use with [extractResponseSchemaFields].
     * Each takes the raw OpenAPI response key (e.g. "200", "2XX", "default") and decides match.
     */
    fun statusGroupMatcher(group: StatusGroup): (String) -> Boolean = { key ->
        key.toIntOrNull()?.let(group::isInGroup) == true
    }

    fun statusCodeMatcher(code: Int): (String) -> Boolean = { it == code.toString() }

    fun statusCodesMatcher(vararg codes: Int): (String) -> Boolean {
        val set = codes.map(Int::toString).toSet()
        return { key -> key in set }
    }

    /**
     * Returns the property names from the request body schema for the given path + verb.
     * Used as a fallback to determine writable fields when no BodyParam is present on the action.
     */
    fun extractRequestBodySchemaFields(
        schema: RestSchema,
        pathString: String,
        verb: HttpVerb
    ): Set<String> {
        val openAPI = schema.main.schemaParsed
        val pathItem = openAPI.paths?.get(pathString) ?: return emptySet()
        val op = pathItemOperation(pathItem, verb) ?: return emptySet()
        val requestBody = op.requestBody ?: return emptySet()
        val mediaType = requestBody.content?.values?.firstOrNull() ?: return emptySet()
        return collectPropertyNames(schema, mediaType.schema)
    }

    /**
     * Returns the property names from a response schema for the given path + verb.
     * The response is selected by [statusMatcher] (defaults to first 2xx); if no response
     * matches and [fallbackToDefault] is true, "default" is used.
     *
     * Convenience matchers: [statusGroupMatcher], [statusCodeMatcher], [statusCodesMatcher].
     */
    fun extractResponseSchemaFields(
        schema: RestSchema,
        pathString: String,
        verb: HttpVerb,
        statusMatcher: (String) -> Boolean = statusGroupMatcher(StatusGroup.G_2xx),
        fallbackToDefault: Boolean = true
    ): Set<String> {
        val openAPI = schema.main.schemaParsed
        val pathItem = openAPI.paths?.get(pathString) ?: return emptySet()
        val op = pathItemOperation(pathItem, verb) ?: return emptySet()

        val response = op.responses?.entries?.firstOrNull { statusMatcher(it.key) }?.value
            ?: (if (fallbackToDefault) op.responses?.get("default") else null)
            ?: return emptySet()

        val mediaType = response.content?.values?.firstOrNull() ?: return emptySet()
        return collectPropertyNames(schema, mediaType.schema)
    }



    /**
     * depending on whether path is a file or folder, return a list with 1 or more Overlays.
     * Suffix check is only done if folder.
     * If path is empty, return null.
     */
    fun readOverlayFiles(path: String, suffixes: String): List<String>? {

        if(path.isBlank()){
            //nothing to do
            return null
        }

        val ap = Paths.get(path).toAbsolutePath()

        if(!ap.exists()){
            throw IllegalArgumentException("Path $path does not point to any existing file or folder")
        }

        if(Files.isRegularFile(ap)){
            //only one file
            LoggingUtil.getInfoLogger().info("Retrieving Overlay from: $path")
            return listOf(ap.readText())
        }

        val options = suffixes.split(',').map { it.trim() }

        LoggingUtil.getInfoLogger().info("Scanning for Overlay files with possible suffix '$suffixes' in $path")

        return ap.walk()
            .filter{file ->  options.any{s ->  file.name.endsWith(s) } }
            .map {
                LoggingUtil.getInfoLogger().info("Retrieving Overlay from: ${it.absolutePathString()}")
                it.readText()
            }
            .toList()
    }
}