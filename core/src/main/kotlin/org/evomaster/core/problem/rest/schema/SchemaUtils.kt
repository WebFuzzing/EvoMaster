package org.evomaster.core.problem.rest.schema


import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.examples.Example
import io.swagger.v3.oas.models.links.Link
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.parameters.Parameter
import io.swagger.v3.oas.models.parameters.RequestBody
import io.swagger.v3.oas.models.responses.ApiResponse
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.problem.rest.data.Endpoint
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

    fun hasAuthDefinition(schema: RestSchema) : Boolean{

        //TODO should handle $ref

        val securitySchemes = schema.main.schemaParsed.components.securitySchemes
        if(securitySchemes == null || securitySchemes.isEmpty()){
            return false
        }

        return  true
    }

    fun getDeclaredStatusInResponse(endpoint: Endpoint, schema: RestSchema) : Set<Int>{

        //TODO should handle $ref

        val pathObject = schema.main.schemaParsed.paths.get(endpoint.path.toString())
            ?: return setOf() //TODO should rather throw exception when handling $ref

        val operations = pathObject.readOperationsMap()
        val verb = PathItem.HttpMethod.valueOf(endpoint.verb.toString())
        val operation = operations[verb]
            ?: return setOf() //TODO should rather throw exception when handling $ref

        return operation.responses.keys
            .mapNotNull { try{it.toInt()} catch (e: Exception){null} }
            .toSet()
    }

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