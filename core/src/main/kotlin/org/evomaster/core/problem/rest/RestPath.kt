package org.evomaster.core.problem.rest

import org.evomaster.core.problem.rest.param.Param
import org.evomaster.core.problem.rest.param.PathParam
import org.evomaster.core.problem.rest.param.QueryParam
import org.evomaster.core.search.gene.OptionalGene
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.URLEncoder

/**
 * Represent a path template for a REST endpoint.
 * For example /foo/{id}
 * A template can be "resolved" by replacing each variable with
 * an actual value (eg, id=42 would give /foo/42)
 */
class RestPath(path: String) {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(RestPath::class.java)
    }


    /*
        The path is split in elements around the delimiter "/".
        Each path element could contain 0, 1 or more variables that need to
        be resolved.
     */

    private data class Element(val tokens: List<Token>) {

        override fun toString(): String {
            return tokens.joinToString("")
        }
    }

    private data class Token(val name: String, val isParameter: Boolean) {

        override fun toString(): String {
            return if (isParameter) {
                "{$name}"
            } else {
                name
            }
        }
    }

    private val elements: List<Element>

    init {
        if (path.contains("?") || path.contains("#")) {
            throw IllegalArgumentException("The path contains invalid characters. " +
                    "Are you sure you didn't pass a full URI?\n$path")
        }

        elements = path.split("/")
                .filter { !it.isBlank() }
                .map { extractElement(it) }
    }


    private fun extractElement(s: String): Element {

        /*
            simple scanning for tokens inside {}.
            This assumes that there can be no nesting, eg no {{{}}{}}
         */

        val tokens = mutableListOf<Token>()

        var next = 0

        while (next < s.length) {
            val current = next
            if (s[next] == '{') {
                next = handleVariable(s, current)
                tokens.add(Token(s.substring(current + 1, next-1), true))
            } else {
                next = handleBase(s, current)
                tokens.add(Token(s.substring(current, next), false))
            }
        }

        return Element(tokens)
    }

    private fun handleBase(s: String, i: Int): Int {
        assert(s[i] != '{')

        val next = s.indexOf("{", i)
        return if (next < 0) {
            s.length
        } else {
            next
        }
    }

    private fun handleVariable(s: String, i: Int): Int {
        assert(s[i] == '{')

        val closing = s.indexOf("}", i)
        return if (closing < 0) {
            throw java.lang.IllegalArgumentException("Opening { but missing closing } in: $s")
        } else {
            closing + 1
        }
    }

    override fun toString(): String {
        return "/" + elements.joinToString("/")
    }

    /**
     * @return the number of distinct elements in the path, ie the
     * hierarchy level
     */
    fun levels() = elements.size

    /**
     * Return ordered list of names of all the Path Parameter variables
     * in this path
     */
    fun getVariableNames(): List<String> {
        return elements.flatMap { it.tokens }
                .filter { it.isParameter }
                .map { it.name }
    }

    fun hasVariablePathParameters(): Boolean {
        return elements.flatMap { it.tokens }.any { it.isParameter }
    }

    fun isEquivalent(other: RestPath): Boolean {
        if (this.elements.size != other.elements.size) {
            return false
        }
        return (0 until elements.size).none { this.elements[it] != other.elements[it] }
    }


    fun lastElement(): String {
        if (elements.isEmpty()) {
            return "root"
        }
        return elements.last().tokens.last().name
    }

    fun isLastElementAParameter(): Boolean {
        if (elements.isEmpty()) {
            return false
        }
        return elements.last().tokens.any { it.isParameter }
    }

    fun isDirectChildOf(other: RestPath): Boolean {
        if (this.elements.size != 1 + other.elements.size) {
            return false
        }

        return other.isAncestorOf(this)
    }


    /**
     * Prefix or same as "other"
     */
    fun isAncestorOf(other: RestPath): Boolean {
        if (this.elements.size > other.elements.size) {
            return false
        }

        return (0 until this.elements.size).none { other.elements[it] != this.elements[it] }
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
     * if the input params have a variable called "id" with value 5.
     * In case the params contains query parameters, those will be resolved
     * and added as well.
     */
    fun resolve(params: List<Param>): String {

        var path = resolveOnlyPath(params)

        val queries = resolveOnlyQuery(params)
        if (queries.isNotEmpty()) {
            path += "?" + queries.joinToString("&")
        }

        return path
    }

    private fun usableQueryParamsFunction(): (Param) -> Boolean {
        return { it is QueryParam && (it.gene !is OptionalGene || it.gene.isActive) }
    }

    fun numberOfUsableQueryParams(params: List<Param>): Int {
        return params.filter(usableQueryParamsFunction()).size
    }

    fun resolveOnlyQuery(params: List<Param>): List<String> {

        return params
                .filter(usableQueryParamsFunction())
                .map {
                    val name = encode(it.name)
                    val gene = it.gene
                    val value = encode(gene.getValueAsRawString())
                    "$name=$value"
                }
    }

    /**
     * Get a string representation of the current path template in which
     * all variables are replaced with the values in the input parameters
     */
    fun resolveOnlyPath(params: List<Param>): String {

        val path = StringBuffer()

        elements.forEach { e ->

            path.append("/")

            e.tokens.forEach { t ->
                if (!t.isParameter) {
                    path.append(t.name)
                } else {
                    val p = params.find { p -> p is PathParam && p.name == t.name }
                            ?: throw IllegalArgumentException("Cannot resolve path parameter '${t.name}'")

                    var value = p.gene.getValueAsRawString()

                    if (value.isBlank()) {
                        /*
                        We should avoid having path params that are blank,
                        as they would easily lead to useless 404/405 errors

                        TODO handle this case better, eg avoid having blank in
                        the first place
                     */
                        value = "1"
                    }

                    path.append(value)
                }
            }
        }

        /*
           reserved characters need to be encoded
           https://tools.ietf.org/html/rfc3986#section-2.2

           why not using URI also for Query part???
           it seems unclear how to properly build it as a single string...
         */

        return URI(null, null, path.toString(), null, null).rawPath
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

    fun copy() : RestPath{
        return RestPath(this.toString())
    }

    /**
     * return extracted tokens which are not parameter in the path
     */
    fun getNonParameterTokens(): List<String> {
        return elements.flatMap { it.tokens.filter { t -> !t.isParameter }.map { t -> t.name } }
    }

    fun getElements() :List<Map<String, Boolean>>{
        return elements.map { it.tokens.map { t->Pair(t.name, t.isParameter) }.toMap() }
    }
}