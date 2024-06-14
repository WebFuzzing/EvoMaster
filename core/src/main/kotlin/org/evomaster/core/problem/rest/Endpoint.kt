package org.evomaster.core.problem.rest

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import org.evomaster.core.config.ConfigProblemException
import java.util.stream.Stream


/**
 * Endpoint declaration, including a verb and a path.
 *
 * Immutable object.
 */
class Endpoint(
    val verb: HttpVerb,
    val path: RestPath
) {
    companion object{

        fun fromOpenApi(schema: OpenAPI): List<Endpoint>{

            return schema.paths
                .flatMap {
                    val e = it.value
                    val path = RestPath(it.key)
                    val list = mutableListOf<Endpoint>()
                    val add = {verb: HttpVerb -> list.add(Endpoint(verb, path))}
                    if (e.get != null)     add(HttpVerb.GET)
                    if (e.post != null)    add(HttpVerb.POST)
                    if (e.put != null)     add(HttpVerb.PUT)
                    if (e.patch != null)   add(HttpVerb.PATCH)
                    if (e.options != null) add(HttpVerb.OPTIONS)
                    if (e.delete != null)  add(HttpVerb.DELETE)
                    if (e.trace != null)   add(HttpVerb.TRACE)
                    if (e.head != null)    add(HttpVerb.HEAD)
                    list
                }
        }

        fun getOperation(verb: HttpVerb, pathItem: PathItem) : Operation{
            return when(verb){
                HttpVerb.GET -> pathItem.get
                HttpVerb.POST -> pathItem.post
                HttpVerb.PUT -> pathItem.put
                HttpVerb.DELETE -> pathItem.delete
                HttpVerb.OPTIONS -> pathItem.options
                HttpVerb.PATCH -> pathItem.patch
                HttpVerb.TRACE -> pathItem.trace
                HttpVerb.HEAD -> pathItem.head
            }
        }

        fun validateTags(tagFilters: List<String>, schema: OpenAPI) {

            val allTags = schema.paths
                .flatMap {
                    it.value.readOperations()
                            .filter { op -> op.tags != null}
                            .flatMap { op -> op.tags }
                }
                .toSet()

            val missing = tagFilters.filter { !allTags.contains(it) }
            if(missing.isNotEmpty()){
                throw ConfigProblemException(
                    "${missing.size} missing tag filters from schema: ${missing.joinToString(",")}." +
                            " Existing tags in the schema are: [${allTags.joinToString(",")}]")
            }
        }

        fun validatePrefix(prefix: String, schema: OpenAPI){
            if(schema.paths.none { it.key.startsWith(prefix) }){
                throw ConfigProblemException("The prefix '$prefix' does not match any endpoint in the schema")
            }
        }

        fun validateFocus(focus: String, schema: OpenAPI){
            if(schema.paths.none { it.key == focus }){
                throw ConfigProblemException("The focus endpoint '$focus' does not match any endpoint in the schema")
            }
        }
    }

    fun getTags(schema: OpenAPI) : List<String>{

        val pathDeclaration = schema.paths[this.path.toString()]
            ?: throw IllegalArgumentException("Input schema has no endpoint declaration for: ${this.path}")
        val op = getOperation(verb, pathDeclaration)

        return op.tags ?: listOf()
    }


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Endpoint

        if (verb != other.verb) return false
        if (path != other.path) return false

        return true
    }

    override fun hashCode(): Int {
        var result = verb.hashCode()
        result = 31 * result + path.hashCode()
        return result
    }

    override fun toString(): String {
        return "$verb:$path"
    }


}