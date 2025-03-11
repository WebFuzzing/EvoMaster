package org.evomaster.core.problem.rest.schema

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
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
    }

    private val otherSpecs = mutableMapOf<String, SchemaOpenAPI>()

    init{
        //need to check for all $ref, recursively

        //TODO enable once fixed and tested
        //handleRefs(main)
    }

    private fun handleRefs(schema: SchemaOpenAPI){

        //https://swagger.io/docs/specification/v3_0/using-ref/

        val mapper = ObjectMapper()
        val tree = mapper.readTree(schema.schemaRaw)
        val refs = findAllSRef(tree)

        refs.forEach {
            if(SchemaUtils.isLocalRef(it)){
                return@forEach
            }
            val rawLocation = SchemaUtils.extractLocation(it)
            val location = computeLocation(rawLocation, schema.sourceLocation)
            if(otherSpecs.containsKey(location)){
                //we already handled it
                return@forEach
            }

            val other = if(schema.sourceLocation.type == SchemaLocationType.RESOURCE){
                OpenApiAccess.getOpenAPIFromResource(location)
            } else {
                OpenApiAccess.getOpenAPIFromLocation(location)
            }

            otherSpecs[location] = other
            handleRefs(other) // recursion, the new fetched schema could use $ref as well
        }
    }

    private fun computeLocation(rawLocation: String, currentSource: SchemaLocation) : String{
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
            if(protocol.isNullOrBlank()){
                log.warn("No protocol can be inferred for $rawLocation from $csl")
            }
            return "$protocol:$rawLocation"
        }

        //if arrive here, it is a relative path
        val delimiter = if(csl.endsWith("/")) "" else "/"
        val parentFolder = "../" // this is based to what discussed in the specs
        return "$csl$delimiter$parentFolder$rawLocation"
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