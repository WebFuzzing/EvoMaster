package org.evomaster.core.problem.rest

import org.evomaster.core.problem.rest.param.Param
import org.evomaster.core.problem.rest.param.PathParam
import org.evomaster.core.problem.rest.param.QueryParam
import org.evomaster.core.search.gene.OptionalGene
import org.slf4j.Logger
import org.slf4j.LoggerFactory


class RestPath(path: String) {

    private data class Token(val name: String, val isParameter: Boolean) {

        override fun toString(): String {
            if (isParameter) {
                return "{$name}"
            } else {
                return name
            }
        }
    }

    private val tokens: List<Token>

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
        return "/" + tokens.map { t -> t.toString() }.joinToString("/")
    }

    fun levels() = tokens.size

    /**
     * Return ordered list of names of all the Path Parameter variables
     * in this path
     */
    fun getVariableNames(): List<String> {
        return tokens.filter { t -> t.isParameter }.map { t -> t.name }
    }

    fun hasVariablePathParameters(): Boolean{
        return tokens.any { t -> t.isParameter }
    }

    fun isEquivalent(other: RestPath): Boolean {
        if (this.tokens.size != other.tokens.size) {
            return false
        }
        for (i in 0 until tokens.size) {
            if (this.tokens[i] != other.tokens[i]) {
                return false
            }
        }
        return true
    }

    fun lastElement(): String {
        if (tokens.isEmpty()) {
            return "root"
        }
        return tokens.last().name
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

        return other.isAncestorOf(this)
    }

    /**
     * Prefix or same as "other"
     */
    fun isAncestorOf(other: RestPath): Boolean {
        if (this.tokens.size > other.tokens.size) {
            return false
        }

        for (i in 0 until this.tokens.size) {
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
            var value: String

            if (!t.isParameter) {
                value = t.name
            } else {
                var p = params.find { p -> p is PathParam && p.name == t.name } ?:
                        throw IllegalArgumentException("Cannot resolve path parameter '${t.name}'")

                value = p.gene.getValueAsString()
                value = value.replace("\"", "")

                if (value.isBlank()) {
                    /*
                        We should avoid having path params that are blank,
                        as they would easily lead to useless 404/405 errors

                        TODO handle this case better, eg avoid having blank in
                        the first place
                     */
                    value = "1"
                }
            }
            path.append("/$value")
        }


        val queries = params.filter { p -> p is QueryParam && (p.gene !is OptionalGene || p.gene.isActive) }
        if (queries.size > 0) {
            path.append("?" +
                    queries.map { q -> q.name + "=" + q.gene.getValueAsString() }
                            .joinToString("&")
            )
        }

        //TODO check all cases for \" replacement, eg when it is fine
        return path.toString().replace("\"", "")
    }


}