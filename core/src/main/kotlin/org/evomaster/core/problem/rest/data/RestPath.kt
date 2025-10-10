package org.evomaster.core.problem.rest.data

import opennlp.tools.stemmer.PorterStemmer
import org.evomaster.core.problem.api.param.Param
import org.evomaster.core.problem.rest.link.RestLinkParameter
import org.evomaster.core.problem.rest.param.PathParam
import org.evomaster.core.problem.rest.param.QueryParam
import org.evomaster.core.search.gene.collection.ArrayGene
import org.evomaster.core.search.gene.wrapper.OptionalGene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.URLEncoder
import java.util.regex.Pattern
import kotlin.math.abs

/**
 * Represent a path template for a REST endpoint.
 * For example /foo/{id}
 * A template can be "resolved" by replacing each variable with
 * an actual value (eg, id=42 would give /foo/42)
 */
class RestPath(path: String) {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(RestPath::class.java)

        private const val DEFAULT_FOR_EMPTY_PATH_ELEMENT = "1"
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

        fun hasParams() = tokens.any { it.isParameter }
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

    //memoized the value, as expensive to compute, called often, and this object is immutable anyway...
    private val computedToString: String

    private val endsWithSlash : Boolean


    /**
     * The qualifying name for this resource endpoint.
     * Typically, it would be the last element of the path.
     * If this is a variable, then we look up to first constant ancestor, and we get its _stem_
     *
     * For example:
     *
     * /users/{id}/balance -> balance
     * /users/{id}         -> user
     * /users              -> users
     */
    val nameQualifier : String

    init {
        if (path.contains("?") || path.contains("#")) {
            throw IllegalArgumentException(
                "The path contains invalid characters. " +
                        "Are you sure you didn't pass a full URI?\n$path"
            )
        }
        if(path.isBlank()){
            throw IllegalArgumentException("Empty path definition")
        }

        endsWithSlash = path.endsWith("/") && path != "/"

        elements = path.split("/")
            .filter { it.isNotBlank() }
            .map { extractElement(it) }

        computedToString = doComputeToString(elements, endsWithSlash)

        nameQualifier = computeNameQualifier()
    }

    private fun doComputeToString(elements: List<Element>, endsWithSlash : Boolean)
        = "/" + elements.joinToString("/") + if(endsWithSlash) "/" else ""

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
                tokens.add(Token(s.substring(current + 1, next - 1), true))
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
        return computedToString
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
        return (elements.indices.none { this.elements[it] != other.elements[it] })
                && this.endsWithSlash == other.endsWithSlash
    }

    fun lengthSharedAncestors(other: RestPath): Int {
        var common = 0
        for(i in elements.indices) {
            if(i >= other.elements.size) {
                return common
            }
            if(elements[i] != other.elements[i]) {
                return common
            }
            common++
        }
        return common
    }

    /**
     * @return whether this is sibling of the [other]
     *
     * eg, we consider
     * /root/{rootName}/foo/{fooName}/bar/{barName}
     * /root/{rootName}/foo/{fooName}/bar are sibling
     *
     * for instance, two examples might be valid for preparing resources for each other.
     * POST /root/{rootName}/foo/{fooName}/bar/{barName}
     * GET /root/{rootName}/foo/{fooName}/bar
     *
     * POST /root/{rootName}/foo/{fooName}/bar
     * GET /root/{rootName}/foo/{fooName}/bar/{barName}
     */
    fun isSiblingForPreparingResource(other: RestPath): Boolean {
        val sizeDif = abs(this.elements.size - other.elements.size)
        if (sizeDif != 1) {
            return false
        }

        val moreSize = if (this.elements.size > other.elements.size) this else other

        return (0 until (moreSize.elements.size - 1)).none { this.elements[it] != other.elements[it] } && moreSize.isLastElementAParameter()
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

        return other.isSameOrAncestorOf(this)
    }


    /**
     * Prefix or same as "other"
     */
    fun isSameOrAncestorOf(other: RestPath): Boolean {
        if (this.elements.size > other.elements.size) {
            return false
        }
        if(this.elements.size == other.elements.size && this.endsWithSlash && !other.endsWithSlash){
            return false
        }

        return (0 until this.elements.size).none { other.elements[it] != this.elements[it] }
    }


    /**
     * @return whether this is direct or possible ancestor of [other]
     *
     * this is to handle the case eg.
     * /root/{rootName}/bar/{barName}
     * might be the potential ancestor of
     * /root/{rootName}/foo/{fooName}/bar/{barName}
     *
     */
    fun isDirectOrPossibleAncestorOf(other: RestPath): Boolean {
        if (this.elements.size > other.elements.size) {
            return false
        }

        return (0 until this.elements.size).all {
            val index = other.elements.indexOfFirst { e-> e == this.elements[it] }
            index >= 0 && index >= it
        }
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
        return { it is QueryParam && it.isActive() }
    }

    fun numberOfUsableQueryParams(params: List<Param>): Int {
        return params.filter(usableQueryParamsFunction()).size
    }

    fun getOnlyUsableQueries(params: List<Param>): List<QueryParam> {
        return params
            .filter(usableQueryParamsFunction())
            .filterIsInstance<QueryParam>()
    }

    fun resolveOnlyQuery(params: List<Param>): List<String> {
        return getOnlyUsableQueries(params)
            .map { q ->
                val name = encode(q.name)

                val gene = q.getGeneForQuery().getLeafGene()
                if(gene is ArrayGene<*> && q.explode && gene.getViewOfElements().isNotEmpty()){
                    gene.getViewOfElements()
                        .joinToString("&") { "$name=${encode(it.getValueAsRawString())}" }
                } else {
                    val value = encode(gene!!.getValueAsRawString())
                    "$name=$value"
                }
            }
    }

    /**
     * Get a string representation of the current path template in which
     * all variables are replaced with the values in the input parameters
     */
    fun resolveOnlyPath(params: List<Param>): String {
        /*
            if there is no variable, then dynamic resolution should return only 1 entry
         */
        val data = dynamicResolutionOnlyPathData(params, mapOf())
        assert(data.size == 1)
        assert(!data[0].second)
        return data[0].first
    }


    /**
     * This is tricky...
     * at times, we want to resolve an endpoint path, but some of its variables could be dynamic.
     * This is the case when "links" in OpenAPI schema are used.
     * For example, a path like
     *
     * /api/{x}/{y}
     *
     * could be resolved as:
     *
     * "/api/" + storedResponseFieldFromAPreviousCall + "/42"
     *
     * but we can't really build such string here, as escaping rules could vary based on context
     * and format (eg Kotlin vs Java for string interpolation)  :(
     *
     * @return a list of pairs: string representation and whether it represents a variable.
     * In the previous example, it would be:
     * ("/api/", false) , ("storedResponseFieldFromAPreviousCall", true), ("/42", false)
     */
    fun dynamicResolutionOnlyPathData(
        params: List<Param>,
        variables: Map<String, RestLinkParameter>
    ) : List<Pair<String,Boolean>>{

        val data = mutableListOf<Pair<String,Boolean>>()

        val path = StringBuffer()

        elements.forEach { e ->

            path.append("/")

            e.tokens.forEach { t ->
                if (!t.isParameter) {
                    path.append(t.name)
                } else {

                    val variable = variables.entries.find {
                        it.value.name == t.name && (it.value.scope == null || it.value.scope == RestLinkParameter.Scope.PATH)
                    }?.key

                    if(variable != null){
                        /*
                            reserved characters need to be encoded
                            https://tools.ietf.org/html/rfc3986#section-2.2

                            why not using URI also for Query part???
                            it seems unclear how to properly build it as a single string...
                         */
                        val entry = URI(null, null, path.toString(), null, null).rawPath
                        data.add(Pair(entry, false))
                        path.setLength(0) // clear it
                        data.add(Pair(variable, true))
                    } else {

                        val p = params.find { p -> p is PathParam && p.name == t.name }
                            ?: throw IllegalArgumentException("Cannot resolve path parameter '${t.name}'")

                        var value = p.primaryGene().getValueAsRawString()

                        if (value.isBlank()) {
                            /*
                            We should avoid having path params that are blank,
                            as they would easily lead to useless 404/405 errors.
                            This shouldn't really happen, as we have length constraints on
                            the query genes...
                            but we might miss something (eg length constraints on regex are not supported, yet).
                        */
                            value = DEFAULT_FOR_EMPTY_PATH_ELEMENT
                        }

                        path.append(value)
                    }
                }
            }
        }

        if(endsWithSlash){
            path.append("/")
        }

        if(path.isNotEmpty()){
            val entry = URI(null, null, path.toString(), null, null).rawPath
            data.add(Pair(entry, false))
        }

        if(data.isEmpty()){
            data.add(Pair("/", false))
        }

        return data
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

    fun copy(): RestPath {
        return RestPath(this.toString())
    }

    /**
     * return extracted tokens which are not parameter in the path
     */
    fun getNonParameterTokens(): List<String> {
        return elements.flatMap { it.tokens.filter { t -> !t.isParameter }.map { t -> t.name } }
    }

    /**
     * return extracted tokens which are parameter in the path
     */
    fun getParameterTokens(): List<String> {
        return elements.flatMap { it.tokens.filter { t -> t.isParameter }.map { t -> t.name } }
    }

    fun getElements(): List<Map<String, Boolean>> {
        return elements.map { it.tokens.associate { t -> Pair(t.name, t.isParameter) } }
    }

    /**
     * Checks whether a resolved path matches the RestPath. The type of the path
     * parameters are not considered for the matching.
     * For example:
     *
     * /foo/bar/5
     *
     * matches the path
     *
     * /foo/bar/{id}
     */
    fun matches(resolvedPath: String): Boolean {
        return resolvedPath.matches(Regex(getRegexToMatch()))
    }

    /**
     * Return a map containing the parameters' names and values, according to the
     * resolvedPath provided. Return null if the resolvedPath is invalid
     */
    fun getKeyValues(resolvedPath: String): Map<String, String>? {
        val keyValues = mutableMapOf<String, String>()
        val keys = getParameterTokens()
        val regexToMatch = getRegexToMatch()

        if (resolvedPath.matches(Regex(regexToMatch))) {
            val matcher = Pattern
                .compile(regexToMatch)
                .matcher(resolvedPath)

            if (matcher.find()) {
                for (i in 1..matcher.groupCount())
                    keyValues[keys[i - 1]] = matcher.group(i)
            }

            return keyValues
        }

        return null
    }

    private fun getRegexToMatch(): String {
        val elementsToMatch = mutableListOf<String>()

        elements.forEach { e ->
            elementsToMatch.add("/")
            e.tokens.forEach { t ->
                if (t.isParameter)
                    elementsToMatch.add("([^/]*)")
                else
                    elementsToMatch.add(t.name)
            }
        }
        if(endsWithSlash){
            elementsToMatch.add("/")
        }

        return "^" + elementsToMatch.joinToString("") + "$"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RestPath

        return computedToString == other.computedToString
    }

    override fun hashCode(): Int {
        return computedToString.hashCode()
    }



    private fun computeNameQualifier() : String {

        val noQualifier = "/"

        if(elements.isEmpty()){
            return noQualifier
        }

        var index = elements.indices.last
        val last = elements[index]
        if(!last.hasParams()){
            return last.toString()
        }

        index--

        while(index >= 0){
            if(elements[index].hasParams()){
                index--
                continue
            }

            val raw = elements[index].toString()
            val stemmer = PorterStemmer()
            return stemmer.stem(raw)
        }

        return noQualifier
    }

    fun isRoot() = levels() == 0

    fun parentPath() : RestPath {
        if(isRoot()){
            throw IllegalStateException("Root has no parent")
        }

        if(endsWithSlash){
            return RestPath(doComputeToString(elements, false))
        }

        val reduced = elements.subList(0, elements.lastIndex)
        val path = doComputeToString(reduced, false)
        return RestPath(path)
    }
}
