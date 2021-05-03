package org.evomaster.core.output.service

import org.evomaster.core.output.CookieWriter
import org.evomaster.core.output.Lines
import org.evomaster.core.output.TokenWriter
import org.evomaster.core.problem.httpws.service.HttpWsAction
import org.evomaster.core.problem.httpws.service.HttpWsCallResult
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.problem.rest.RestCallResult
import org.evomaster.core.problem.rest.param.HeaderParam
import org.evomaster.core.search.ActionResult
import org.evomaster.core.search.gene.GeneUtils

abstract class HttpWsTestCaseWriter : WebTestCaseWriter(){

    override fun shouldFailIfException(result: ActionResult) : Boolean{
        /*
            Fail test if exception is not thrown, but not if it was a timeout,
            otherwise the test would become flaky
        */
        return !(result as HttpWsCallResult).getTimedout()
    }

    protected fun createUniqueResponseVariableName(): String {
        val name = "res_$counter"
        counter++
        return name
    }

    open fun needsResponseVariable(call: HttpWsAction, res: HttpWsCallResult): Boolean {
        return false; //TODO check format for C# and JS
    }

    protected fun handleHeaders(call: HttpWsAction, lines: Lines) {

        val prechosenAuthHeaders = call.auth.headers.map { it.name }

        val set = when {
            format.isJavaOrKotlin() -> "header"
            format.isJavaScript() -> "set"
            else -> throw IllegalArgumentException("Not supported format: $format")
        }

        call.auth.headers.forEach {
            lines.add(".$set(\"${it.name}\", \"${it.value}\") // ${call.auth.name}")
        }

        call.parameters.filterIsInstance<HeaderParam>()
                .filter { !prechosenAuthHeaders.contains(it.name) }
                .filter { !(call.auth.jsonTokenPostLogin != null && it.name.equals("Authorization", true)) }
                .forEach {
                    lines.add(".$set(\"${it.name}\", ${it.gene.getValueAsPrintableString(targetFormat = format)})")
                }

        val cookieLogin = call.auth.cookieLogin
        if (cookieLogin != null) {
            when {
                format.isJavaOrKotlin() -> lines.add(".cookies(${CookieWriter.cookiesName(cookieLogin)})")
                format.isJavaScript() -> lines.add(".set('Cookies', ${CookieWriter.cookiesName(cookieLogin)})")
            }
        }

        //TODO make sure header was not already set
        val tokenLogin = call.auth.jsonTokenPostLogin
        if (tokenLogin != null) {
            lines.add(".$set(\"Authorization\", ${TokenWriter.tokenName(tokenLogin)}) // ${call.auth.name}")
        }
    }


    protected fun handleFieldValues(resContentsItem: Any?): String {
        if (resContentsItem == null) {
            return "nullValue()"
        } else {
            when (resContentsItem::class) {
                Double::class -> return "numberMatches(${resContentsItem as Double})"
                String::class -> return "containsString(\"${
                    GeneUtils.applyEscapes(
                            resContentsItem as String,
                            mode = GeneUtils.EscapeMode.ASSERTION,
                            format = format
                    )
                }\")"
                Map::class -> return NOT_COVERED_YET
                ArrayList::class -> if ((resContentsItem as ArrayList<*>).all { it is String } && resContentsItem.isNotEmpty()) {
                    return "hasItems(${
                        (resContentsItem as ArrayList<String>).joinToString {
                            "\"${
                                GeneUtils.applyEscapes(
                                        it,
                                        mode = GeneUtils.EscapeMode.ASSERTION,
                                        format = format
                                )
                            }\""
                        }
                    })"
                } else {
                    return NOT_COVERED_YET
                }
                else -> return NOT_COVERED_YET
            }
        }
        /* BMR: the code above is due to a somewhat unfortunate problem:
        - Gson parses all numbers as Double
        - Hamcrest has a hard time comparing double to int
        The solution is to use an additional content matcher that can be found in NumberMatcher. This can also
        be used as a template for adding more matchers, should such a step be needed.
        * */
    }

    protected fun handleMapLines(index: Int, map: Map<*, *>, lines: Lines) {
        map.keys.forEach {
            val printableTh = handleFieldValues(map[it])
            if (printableTh != "null"
                    && printableTh != NOT_COVERED_YET
                    && !printableTh.contains("logged")
            ) {
                lines.add(".body(\"\'$it\'\", hasItem($printableTh))")
            }
        }
    }




    protected fun addObjectAssertions(resContents: Map<*, *>, lines: Lines) {
        if (resContents.isEmpty()) {
            if (format.isKotlin()) lines.add(".body(\"isEmpty()\", `is`(true))")
            else lines.add(".body(\"isEmpty()\", is(true))")
        }

        val flatContent = flattenForAssert(mutableListOf<String>(), resContents)
        // Removed size checks for objects.
        flatContent.keys
                .filter { !it.contains("timestamp") } //needed since timestamps will change between runs
                .filter { !it.contains("self") } //TODO: temporary hack. Needed since ports might change between runs.
                .forEach {
                    val stringKey = it.joinToString(prefix = "\'", postfix = "\'", separator = "\'.\'")
                    val actualValue = flatContent[it]
                    val printableFieldValue = handleFieldValues(actualValue)
                    if (printSuitable(printableFieldValue)) {
                        /*
                            There are some fields like "id" which are often non-deterministic,
                            which unfortunately would lead to flaky tests
                         */
                        if (stringKey != "\'id\'") lines.add(".body(\"${stringKey}\", ${printableFieldValue})")
                    }
                    //handle additional properties for array
                    handleAdditionalFieldValues(stringKey, actualValue)?.forEach {
                        if (printSuitable(it.second) && it.first != "\'id\'")
                            lines.add(".body(\"${it.first}\", ${it.second})")
                    }
                }


        /* TODO: BMR - We want to avoid time-based fields (timestamps and the like) as they could lead to flaky tests.
        * Even relatively minor timing changes (one second either way) could cause tests to fail
        * as a result, we are now avoiding generating assertions for fields explicitly labeled as "timestamp"
        * Note that this is a temporary (and somewhat hacky) solution.
        * A more elegant and permanent solution could be handled via the flaky test handling (when that will be ready).
        *
        * NOTE: if we have chained locations, then the "id" should be taken from the chained id rather than the test case?
        */
    }

    /**
     * The purpose of the [flattenForAssert] method is to prepare an object for assertion generation.
     * Objects in Responses may be somewhat complex in structure. The goal is to make a map that contains all the
     * leaves of the object, along with the path of keys to get to them.
     *
     * For example, .body("page.size", numberMatches(20.0)) -> in the payload, access the page field, the size field,
     * and assert that the value there is 20.
     */
    protected fun flattenForAssert(k: MutableList<*>, v: Any): Map<MutableList<*>, Any?> {
        val returnMap = mutableMapOf<MutableList<*>, Any?>()
        if (v is Map<*, *>) {
            v.forEach { key, value ->
                if (value == null) {
                    //Man: we might also add key with null here
                    returnMap.putIfAbsent(k.plus(key) as MutableList<*>, null)
                    return@forEach
                } else {
                    val innerkey = k.plus(key) as MutableList
                    val innerMap = flattenForAssert(innerkey, value)
                    returnMap.putAll(innerMap)
                }
            }
        } else {
            returnMap[k] = v
        }
        return returnMap
    }

    protected fun isVerbWithPossibleBodyPayload(verb: String): Boolean {

        val verbs = arrayOf("post", "put", "patch")

        if (verbs.contains(verb.toLowerCase()))
            return true;
        return false;
    }

    /**
     * handle field which is array<Map> with additional assertions, e.g., size
     * @return a list of key of the field and value of the field to be asserted
     */
    protected fun handleAdditionalFieldValues(stringKey: String, resContentsItem: Any?): List<Pair<String, String>>?{
        resContentsItem?: return null
        val list = mutableListOf<Pair<String, String>>()
        when (resContentsItem::class) {
            ArrayList::class -> {
                list.add("$stringKey.size()" to "equalTo(${(resContentsItem as ArrayList<*>).size})")
                resContentsItem.forEachIndexed { index, v ->
                    if (v is Map<*, *>){
                        val flatContent = flattenForAssert(mutableListOf<String>(), v)
                        flatContent.keys
                                .filter { !it.contains("timestamp")  && !it.contains("self")}
                                .forEach {key->
                                    val fstringKey = key.joinToString(prefix = "\'", postfix = "\'", separator = "\'.\'")
                                    val factualValue = flatContent[key]

                                    val key = "$stringKey.get($index).$fstringKey"
                                    list.add(key to handleFieldValues(factualValue))
                                    handleAdditionalFieldValues(key, factualValue)?.let { list.addAll(it) }
                                }
                    }
                }
            }
        }

        return list
    }
}