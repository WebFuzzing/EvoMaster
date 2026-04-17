package org.evomaster.core.problem.rest.schema

import io.swagger.v3.oas.models.OpenAPI
import org.slf4j.LoggerFactory

class SchemaOpenAPI(
    /**
     * The actual raw value of the schema file, as a string
     */
    val schemaRaw: String,
    /**
     * A parsed schema
     */
    val schemaParsed: OpenAPI,
    /**
     * information about the location the schema was retrieved from, e.g.,
     * from file, URL or in memory in our tests.
     */
    val sourceLocation: SchemaLocation
) {

    companion object {
        private val log = LoggerFactory.getLogger(SchemaOpenAPI::class.java)
    }


    fun withOverlays(overlays: List<String>?, lenient: Boolean): SchemaOpenAPI {
        if (overlays.isNullOrEmpty()) {
            return this
        }

        val infoForLenient = "If this behavior is expected, you need to explicitly use '--overlayLenient true'" +
                " to avoid EvoMaster from stopping."

        var modified = schemaRaw
        overlays.forEach {
            val res = try{
                OverlayJVM.applyOverlay(modified,it)
            } catch (e : Exception){
                val message = "Failed to apply overlay transformation: ${e.message}"
                if(lenient){
                    log.warn(message)
                    return@forEach
                } else {
                    throw IllegalArgumentException("$message\n$infoForLenient",e)
                }
            }

            if(res.warnings.isNotEmpty()){
                val message = "Issues (${res.warnings.size}) with applying Overlay transformation:\n${res.warnings.joinToString("\n")}"
                if(lenient){
                    log.warn(message)
                    //here we do not return, as we still apply the partial transformations
                } else {
                    throw IllegalArgumentException("$message\n$infoForLenient")
                }
            }

            modified = res.transformedSchema
        }

        return OpenApiAccess.parseOpenApi(modified, this.sourceLocation)
    }


}