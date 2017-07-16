package org.evomaster.core.problem.rest

import org.evomaster.core.problem.rest.param.Param
import org.evomaster.core.problem.rest.param.PathParam
import org.evomaster.core.problem.rest.param.QueryParam
import org.evomaster.core.search.gene.OptionalGene
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.URLEncoder


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
        if (path.contains("?") || path.contains("#")) {
            throw IllegalArgumentException("The path contains invalid characters. " +
                    "Are you sure you didn't pass a full URI?\n$path")
        }

        tokens = path.split("/").filter { s -> !s.isBlank() }
                .map { s ->
                    /*
                        TODO: technically, leading spaces are valid in a URI.
                        Furthermore, need to check if things like /x-{id} are possible,
                        and how Swagger handle endpoint encoding
                     */
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

    /**
     * @return the number of distinct elements in the path, ie the
     * hierarchy level
     */
    fun levels() = tokens.size

    /**
     * Return ordered list of names of all the Path Parameter variables
     * in this path
     */
    fun getVariableNames(): List<String> {
        return tokens.filter { t -> t.isParameter }.map { t -> t.name }
    }

    fun hasVariablePathParameters(): Boolean {
        return tokens.any { t -> t.isParameter }
    }

    fun isEquivalent(other: RestPath): Boolean {
        if (this.tokens.size != other.tokens.size) {
            return false
        }
        return (0 until tokens.size).none { this.tokens[it] != other.tokens[it] }
    }

    fun isAResolvedOf(other: RestPath) : Boolean {
        if (this.tokens.size != other.tokens.size) {
            return false
        }

        return (0 until tokens.size).none {
            val tt =  this.tokens[it]
            val ot = other.tokens[it]
            tt.isParameter || (tt != ot && !ot.isParameter)
        }
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

        return (0 until this.tokens.size).none { other.tokens[it] != this.tokens[it] }
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

        var path = resolveOnlyPath(params)

        val queries = resolveOnlyQuery(params)
        if (queries.size > 0) {
            path += "?" + queries.joinToString("&")
        }

        return path
    }

    private fun usableQueryParamsFunction(): (Param) -> Boolean {
        return { p -> p is QueryParam && (p.gene !is OptionalGene || p.gene.isActive) }
    }

    fun numberOfUsableQueryParams(params: List<out Param>): Int {
        return params.filter(usableQueryParamsFunction()).size
    }

    fun resolveOnlyQuery(params: List<out Param>): List<String> {

        return params
                .filter(usableQueryParamsFunction())
                .map { q ->
                    val name = encode(q.name)
                    val gene = q.gene
                    val value = encode(gene.getValueAsRawString())
                    "$name=$value"
                }
    }

    fun resolveOnlyPath(params: List<out Param>): String {

        var path = StringBuffer()
        tokens.forEach { t ->
            var value: String

            if (!t.isParameter) {
                value = t.name
            } else {
                var p = params.find { p -> p is PathParam && p.name == t.name } ?:
                        throw IllegalArgumentException("Cannot resolve path parameter '${t.name}'")

                value = p.gene.getValueAsRawString()

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

        /*
           reserved characters need to be encoded
           https://tools.ietf.org/html/rfc3986#section-2.2

           why not using URI also for Query part???
           it seems unclear how to properly build it as a single string...
         */
        path = StringBuffer(URI(null, null, path.toString(), null, null).rawPath)

        return path.toString()
    }


    /**
     * URIs query elements need to be encoded, eg space " " turns into a +,
     * and other symbols get into the %XX hexadecimal format
     *
     * Encoding of query parameters can be quite tricky... good
     * discussion is at:
     * http://stackoverflow.com/questions/1547899/which-characters-make-a-url-invalid
     * http://stackoverflow.com/questions/1634271/url-encoding-the-space-character-or-20
     */
    private fun encode(s: String): String {
        return URLEncoder.encode(s, "UTF-8")
    }
}