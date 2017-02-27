package org.evomaster.core.problem.rest

import org.evomaster.core.problem.rest.param.Param
import org.evomaster.core.problem.rest.param.PathParam
import org.evomaster.core.problem.rest.param.QueryParam
import org.slf4j.Logger
import org.slf4j.LoggerFactory


class RestPath(path: String) {

    data class Token(val name: String, val isParameter: Boolean)

    val tokens: List<Token>

    init {
        tokens = path.split("/").filter { s -> !s.isBlank() }
                .map { s ->
                    val trimmed = s.trim()
                    if (trimmed.startsWith("{")) {
                        if (!trimmed.endsWith("}")) {
                            throw IllegalArgumentException("Opening { was not matched by closing } in: $path")
                        }
                        Token(trimmed.substring(1, trimmed.lastIndex), true)
                    } else {
                        Token(trimmed, false)
                    }
                }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(RestPath::class.java)
    }

    override fun toString(): String {
        return "/" + tokens.map { t -> t.name }.joinToString("/")
    }

    fun getVariableNames(): List<String> {
        return tokens.filter { t -> t.isParameter }.map { t -> t.name }
    }

    fun isEquivalent(other: RestPath): Boolean {
        if (this.tokens.size != other.tokens.size) {
            return false
        }
        for (i in 0 until tokens.size) {
            if (this.tokens[i] != other.tokens[i]) {
                return false;
            }
        }
        return true
    }


    fun isLastElementAParameter(): Boolean {
        if (tokens.isEmpty()) {
            return false
        }
        return tokens.last().isParameter
    }

    fun isDirectChildOf(other: RestPath): Boolean {
        if (this.tokens.size != 1 + other.tokens.size) {
            return false
        }

        for (i in 0 until other.tokens.size) {
            if (other.tokens[i] != this.tokens[i]) {
                return false
            }
        }

        return true
    }

    /**
     * Return a resolved path (starting with "/") based on input parameters.
     * For example:
     *
     * foo/bar/{id}
     *
     * will be resolved into
     *
     * /foo/bar/5
     *
     * if the input params have a variable called "id" with value 5
     */
    fun resolve(params: List<out Param>): String {

        var path = StringBuffer()
        tokens.forEach { t ->
            val value: String

            if (!t.isParameter) {
                value = t.name
            } else {
                var p = params.find { p -> p is PathParam && p.name == t.name } ?:
                        throw IllegalArgumentException("Cannot resolve path parameter '${t.name}'")

                value = p.gene.getValueAsString()
            }
            path.append("/$value")
        }


        val queries = params.filter { p -> p is QueryParam }
        if (queries.size > 0) {
            path.append("?" +
                    queries.map { q -> q.name + "=" + q.gene.getValueAsString() }
                            .joinToString("&")
            )
        }

        return path.toString()
    }


}