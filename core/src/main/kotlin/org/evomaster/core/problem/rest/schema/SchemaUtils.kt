package org.evomaster.core.problem.rest.schema


import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.links.Link
import java.net.URI
import java.net.URISyntaxException

/**
 * https://swagger.io/docs/specification/v3_0/using-ref/
 * https://swagger.io/docs/specification/v3_0/components/
 * https://swagger.io/specification/   (Components Object)
 */
object SchemaUtils {

    private val log = org.slf4j.LoggerFactory.getLogger(SchemaUtils::class.java)


    fun isLocalRef(sref: String) = sref.startsWith("#")

    private fun extractLocation(sref: String) : String{
        if(!sref.contains("#")){
            //FIXME add to messages, no exception
            throw IllegalArgumentException("Not a valid \$ref, as it contains no #: $sref")
        }
        return sref.substring(0, sref.indexOf("#"))
    }


    fun computeLocation(ref: String, currentSource: SchemaLocation) : String{

        val rawLocation = extractLocation(ref)

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
        return "$csl$delimiter$parentFolder$rawLocation"
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

    fun getReferenceLink(schema: RestSchema, current: SchemaOpenAPI, reference: String, messages: MutableList<String>) : Link?{
        val name = extractReferenceName(reference,messages)

        if(isLocalRef(reference)) {
            val link = current.schemaParsed.components.links[name]
            if (link == null) {
                messages.add("Cannot find reference to link: $reference")
            }
            return link
        } else {

            val location = computeLocation(reference, current.sourceLocation)

            return null //TODO
        }
    }
}