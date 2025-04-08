package org.evomaster.core.problem.rest.schema

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import org.evomaster.core.remote.SutProblemException


/**
 * Given a schema file, the whole schema might not be included in just it.
 * A schema could have references to other schemas.
 * All those would need to be automatically retrieved and parsed.
 */
class RestSchema(
    val main: SchemaOpenAPI
) {

    companion object{
        private val log = org.slf4j.LoggerFactory.getLogger(RestSchema::class.java)

        fun fromLocation(location: String) = RestSchema(OpenApiAccess.getOpenAPIFromLocation(location))

        fun fromResource(path: String) = RestSchema(OpenApiAccess.getOpenAPIFromResource(path))
    }

    /**
     * Key -> location
     */
    private val otherSpecs = mutableMapOf<String, SchemaOpenAPI>()

    /**
     * Locations of all other specs that we failed to retrieve
     */
    private val failedRetrievedSpecLocations = mutableSetOf<String>()

    init{
        //need to check for all $ref, recursively
        handleRefs(main)
    }

    fun hasExternalRefs() = otherSpecs.isNotEmpty()

    fun getSpec(location: String) : SchemaOpenAPI?{
        if(failedRetrievedSpecLocations.contains(location)){
            return null
        }
        if(otherSpecs.containsKey(location)){
            return otherSpecs[location]
        }
        if(main.sourceLocation.location == location){
            return main
        }

        //if we arrive here, it is likely a bug in EvoMaster
        throw IllegalArgumentException("Unrecognized location: $location")
    }


    private fun handleRefs(schema: SchemaOpenAPI){

        //https://swagger.io/docs/specification/v3_0/using-ref/

        val mapper = ObjectMapper(YAMLFactory())
        val tree = mapper.readTree(schema.schemaRaw)
        val refs = findAllSRef(tree)

        refs.forEach {
            if(SchemaUtils.isLocalRef(it)){
                return@forEach
            }

            val location = SchemaUtils.computeLocation(it, schema.sourceLocation, mutableListOf())
                ?: return@forEach

            if(otherSpecs.containsKey(location) || failedRetrievedSpecLocations.contains(location)){
                //we already handled it
                return@forEach
            }

            val other = try{
                if(schema.sourceLocation.type == SchemaLocationType.RESOURCE){
                    OpenApiAccess.getOpenAPIFromResource(location)
                } else {
                    OpenApiAccess.getOpenAPIFromLocation(location)
                }
            }catch (e: Exception){
                //TODO should it go to messages
                log.warn("Failed to retrieve spec at $location")
                failedRetrievedSpecLocations.add(location)
                return@forEach
            }

            otherSpecs[location] = other
            handleRefs(other) // recursion, the new fetched schema could use $ref as well
        }
    }



    private fun findAllSRef(node: JsonNode): List<String> {
        val refNodes = mutableListOf<JsonNode>()
        findRefNodesRecursive(node, refNodes)
        return refNodes.map{it.textValue()}
    }

    private fun findRefNodesRecursive(node: JsonNode, refNodes: MutableList<JsonNode>) {
        val sref =  "\$ref"

        if (node.isObject) {
            if (node.has(sref)) {
                refNodes.add(node.get(sref))
            }
            node.fields().forEach { (_, value) ->
                findRefNodesRecursive(value, refNodes)
            }
        } else if (node.isArray) {
            node.forEach { element ->
                findRefNodesRecursive(element, refNodes)
            }
        }
    }


    fun validate(){
        if (main.schemaParsed.paths == null) {
            throw SutProblemException("There is no endpoint definition in the retrieved OpenAPI file")
        }
        // give the error message if there is nothing to test
        if (main.schemaParsed.paths.size == 0){
            throw SutProblemException("The OpenAPI file from ${main.sourceLocation} " +
                    "is either invalid or it does not define endpoints")
        }
    }
}