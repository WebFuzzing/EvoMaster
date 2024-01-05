package org.evomaster.core.problem.rest

import io.swagger.v3.oas.models.OpenAPI
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