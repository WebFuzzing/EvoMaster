package org.evomaster.core.problem.httpws

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.google.common.annotations.VisibleForTesting
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import org.evomaster.core.problem.enterprise.EnterpriseActionResult
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.core.utils.FlakinessInferenceUtil
import javax.ws.rs.core.MediaType

abstract class HttpWsCallResult : EnterpriseActionResult {

    constructor(sourceLocalId: String, stopping: Boolean = false) : super(sourceLocalId,stopping)

    @VisibleForTesting
    internal constructor(other: HttpWsCallResult) : super(other) {
        flakyObservations.addAll(other.flakyObservations)
    }

    companion object {
        private val mapper = ObjectMapper()

        const val STATUS_CODE = "STATUS_CODE"
        const val BODY = "BODY"
        const val BODY_TYPE = "BODY_TYPE"
        const val ERROR_MESSAGE = "ERROR_MESSAGE"
        const val TOO_LARGE_BODY = "TOO_LARGE_BODY"
        const val INFINITE_LOOP = "INFINITE_LOOP"
        const val TIMEDOUT = "TIMEDOUT"
        const val LAST_STATEMENT_WHEN_500 = "LAST_STATEMENT_WHEN_500"
        const val TCP_PROBLEM = "TCP_PROBLEM"
        const val INVALID_HTTP = "INVALID_HTTP"
        const val APPLIED_LINK = "APPLIED_LINK"
        const val LOCATION = "LOCATION"
        const val ALLOW = "ALLOW"
        const val RESPONSE_TIME_MS = "RESPONSE_TIME_MS"

        const val VULNERABLE_SSRF = "VULNERABLE_SSRF"
        const val VULNERABLE_SQLI = "VULNERABLE_SQLI"


    }

    /**
     * this is used to record observed flakiness during flakiness handling
     */
    private val flakyObservations: MutableList<FlakyObservation> = mutableListOf()


    /**
     * In some cases (eg infinite loop redirection), a HTTP call
     * might fail, and, as such, we might not have an actual "result"
     * object with info
     */
    fun failedCall(): Boolean{
        return getInfiniteLoop() || getTimedout() || getTcpProblem() || getInvalidHTTP()
    }


    /**
     * This is VERY TRICKY.
     * We can have a failedCall()... as well as a call that works fine in EM,
     * but then "fail" in the generated tests.
     * This depends on how different libraries dealing with HTTP handle invalid HTTP messages.
     * For example, Jersey throw an exception if below 100, but works fine if 600 or above.
     * RestAssured throws exceptions even in this latter case.
     * No idea (not checked) what the libraries in JS and Python do.
     * So, here we mark as "invalid" (equivalent to "failed") even successful calls, as long
     * as their status is invalid.
     *
     * To make things even more than the shitshow it is already, technically speaking the range 600-999 is not
     * invalid according to RFC specs... (it should be just treated as 5xx)
     */
    fun invalidCall(): Boolean{
        return failedCall() || invalidStatus()
    }

    fun invalidStatus() : Boolean{
        val statusCode = getStatusCode()
        if(statusCode != null && statusCode !in 100..<600){
            return true
        }
        //in theory, could be other cases that leads to an invalid HTTP besides the status
        return getInvalidHTTP()
    }

    fun setStatusCode(code: Int) {
        // We can't have this check here... it would crash if API sends a wrong code,
        // which now we actually do in some E2E tests...
//        if (code !in 100..<600) {
//            throw IllegalArgumentException("Invalid HTTP code $code")
//        }

        addResultValue(STATUS_CODE, code.toString())
    }

    fun getStatusCode(): Int? = getResultValue(STATUS_CODE)?.toInt()

    fun setLocation(location: String?) {
        if (location != null) {
            addResultValue(LOCATION, location)
        }
    }

    fun getLocation(): String? = getResultValue(LOCATION)

    fun setAllow(allow: String?){
        if(allow != null) {
            addResultValue(ALLOW, allow)
        }
    }

    fun getAllow(): String? = getResultValue(ALLOW)

    /**
     * Return verbs based on "allow" header.
     * It can return null to indicate there was no allow header.
     */
    fun getAllowedVerbs() : Set<HttpVerb>?{
        val allow = getAllow() ?: return null
        val fromAllow = allow.split(",")
            .mapNotNull {
                try{
                    HttpVerb.valueOf(it.trim())
                } catch (e: IllegalArgumentException){
                    //a bug, but we do not check this here
                    null
                }
            }
        return fromAllow.toSet()
    }

    fun hasErrorCode() : Boolean = getStatusCode()!=null && getStatusCode()!! >= 500

    /*
        FIXME should rather be a byte[]
     */
    fun setBody(body: String){

        /*
            Try to infer if there is any error message in the body response.
            TODO this is very limited and adhoc. Should be refactored/extended with
            more heuristics.
         */
        var errorMsg : String? = null;
        val bodyType = getBodyType()
        if(getStatusCode() == 500
            && (bodyType != null && bodyType.isCompatible(MediaType.APPLICATION_JSON_TYPE))) {
            errorMsg = try {
                Gson().fromJson(body, Map::class.java)?.get("message").toString() ?: ""
            } catch (e: JsonSyntaxException) {
                null
            }
        }
        if(errorMsg != null){
            addResultValue(ERROR_MESSAGE, errorMsg)
        }

        return addResultValue(BODY, body)
    }

    fun getBody(): String? = getResultValue(BODY)

    fun getErrorMsg() : String? = getResultValue(ERROR_MESSAGE)

    fun setBodyType(bodyType: MediaType) = addResultValue(BODY_TYPE, bodyType.toString())
    fun getBodyType(): MediaType? {
        return getResultValue(BODY_TYPE)?.let { MediaType.valueOf(it) }
    }

    fun setTooLargeBody(on: Boolean) = addResultValue(TOO_LARGE_BODY, on.toString())
    fun getTooLargeBody(): Boolean = getResultValue(TOO_LARGE_BODY)?.toBoolean() ?: false

    fun hasBody() : Boolean{
        return !getBody().isNullOrEmpty() && !getTooLargeBody()
    }

    fun setInfiniteLoop(on: Boolean) = addResultValue(INFINITE_LOOP, on.toString())
    fun getInfiniteLoop(): Boolean = getResultValue(INFINITE_LOOP)?.toBoolean() ?: false

    fun setTimedout(timedout: Boolean) = addResultValue(TIMEDOUT, timedout.toString())
    fun getTimedout(): Boolean = getResultValue(TIMEDOUT)?.toBoolean() ?: false

    fun setLastStatementWhen500(statement: String) {
        if(getStatusCode() != 500){
            throw IllegalArgumentException("Can set last statement only if status code is registered as 500. " +
                    "Current is " + getStatusCode())
        }
        addResultValue(LAST_STATEMENT_WHEN_500, statement)
    }
    fun getLastStatementWhen500() : String? = getResultValue(LAST_STATEMENT_WHEN_500)

    fun setTcpProblem(tcpProblem: Boolean) = addResultValue(TCP_PROBLEM, tcpProblem.toString())
    fun getTcpProblem() : Boolean = getResultValue(TCP_PROBLEM)?.toBoolean() ?: false

    fun setInvalidHTTP(invalidHTTP: Boolean) = addResultValue(INVALID_HTTP, invalidHTTP.toString())
    fun getInvalidHTTP() : Boolean = getResultValue(INVALID_HTTP)?.toBoolean() ?: false


    fun setAppliedLink(applied: Boolean) = addResultValue(APPLIED_LINK, applied.toString())
    fun getAppliedLink(): Boolean = getResultValue(APPLIED_LINK)?.toBoolean() ?: false

    /**
     * TODO: When dealing with additional vulnerabilities, consider changing the data structure.
     */
    fun setVulnerableForSSRF(on: Boolean) = addResultValue(VULNERABLE_SSRF, on.toString())
    fun getVulnerableForSSRF() : Boolean = getResultValue(VULNERABLE_SSRF)?.toBoolean() ?: false

    fun setVulnerableForSQLI(on: Boolean) = addResultValue(VULNERABLE_SQLI, on.toString())
    fun getVulnerableForSQLI() : Boolean = getResultValue(VULNERABLE_SQLI)?.toBoolean() ?: false

    fun setResponseTimeMs(responseTime: Long) = addResultValue(RESPONSE_TIME_MS, responseTime.toString())
    fun getResponseTimeMs(): Long? = getResultValue(RESPONSE_TIME_MS)?.toLong()

    // -------------------- Flakiness handling --------------------

    /**
     * Fields in responses compared across multiple executions of the http call action
     * when handling flakiness.
     */
    enum class ResponseField {
        STATUS_CODE,
        BODY,
        BODY_TYPE,
        ERROR_MESSAGE //,
        // do not consider the following fields in flakiness handling
//        TOO_LARGE_BODY,
//        INFINITE_LOOP,
//        TIMEDOUT,
//        TCP_PROBLEM,
//        INVALID_HTTP,
//        LOCATION,
//        ALLOW,
//        RESPONSE_TIME_MS
    }

    /**
     * specify how flakiness was observed or identified.
     */
    enum class FlakyObservationSource {
        /**
         * flakiness was observed by re-executing the HTTP action.
         */
        RE_EXECUTION,

        /**
         * flakiness was inferred through static analysis.
         */
        STATIC_INFERENCE
    }

    /**
     * A set of response fields whose values differ from the original call result.
     *
     * [execIndex] is index of re-execution to detect flakiness
     * and is null when the observation comes from static inference.
     * [source] is the source how this flaky was observed
     */
    data class FlakyObservation(
        val source: FlakyObservationSource,
        val execIndex: Int?,
        val differences: Map<ResponseField, String?>
    )

    /**
     * field value differences between the original result and re-execution results.
     */
    data class FieldVariation(
        val field: ResponseField,
        val valuesByExecIndex: Map<Int, String?>
    )


    /**
     * Compare [other] result with [this] original call result
     * the response fields whose observed values differ from the [this] original call result.
     *
     * @param other result from the re-execution to compare against this one
     * @param execIndex index identifying the re-execution that produced [other]
     */
    fun recordFlakyObservation(other: HttpWsCallResult, execIndex: Int) {
        val differences = responseFieldExtractors()
            .mapNotNull { spec ->
                val baseline = spec.extract(this)
                val observed = spec.extract(other)

                if (observed != null && observed != baseline) {
                    spec.field to observed
                } else {
                    null
                }
            }
            .toMap()

        if (differences.isNotEmpty()) {
            flakyObservations.removeIf { it.source == FlakyObservationSource.RE_EXECUTION && it.execIndex == execIndex }
            flakyObservations.add(FlakyObservation(FlakyObservationSource.RE_EXECUTION, execIndex, differences))
        }
    }

    /**
     * Infer unstable values directly from this result, for example by
     * normalizing timestamps or generated identifiers found in response text.
     */
    fun recordStaticFlakyInference() {
        val differences = mutableMapOf<ResponseField, String?>()

        getBody()?.let {
            val normalized = FlakinessInferenceUtil.derive(it)
            if (normalized != it) {
                differences[ResponseField.BODY] = normalized
            }
        }

        getErrorMessage()?.let {
            val normalized = FlakinessInferenceUtil.derive(it)
            if (normalized != it) {
                differences[ResponseField.ERROR_MESSAGE] = normalized
            }
        }

        if (differences.isNotEmpty()) {
            flakyObservations.removeIf { it.source == FlakyObservationSource.STATIC_INFERENCE }
            flakyObservations.add(FlakyObservation(FlakyObservationSource.STATIC_INFERENCE, null, differences))
        }
    }

    /**
     * Return all recorded flaky observations.
     */
    fun getFlakyObservations(): List<FlakyObservation> = flakyObservations.toList()

    /**
     * Return the flaky observation recorded for a specific re-execution.
     */
    fun getFlakyObservation(execIndex: Int): FlakyObservation? =
        flakyObservations.find { it.source == FlakyObservationSource.RE_EXECUTION && it.execIndex == execIndex }

    /**
     * Return the flaky observation derived without re-executing the action.
     */
    fun getStaticFlakyObservation(): FlakyObservation? =
        flakyObservations.find { it.source == FlakyObservationSource.STATIC_INFERENCE }

    /**
     * Check whether any flaky observation contains a difference for [field].
     */
    fun hasFlakyField(field: ResponseField): Boolean =
        flakyObservations.any { it.differences.containsKey(field) }

    /**
     * Return the observed flaky values for [field], ordered by observation
     * source and execution index.
     */
    fun getFlakyValues(field: ResponseField): List<String?> =
        flakyObservations
            .sortedWith(compareBy<FlakyObservation> { it.source }.thenBy { it.execIndex ?: Int.MAX_VALUE })
            .mapNotNull { observation ->
                if (observation.differences.containsKey(field)) {
                    observation.differences[field]
                } else {
                    null
                }
            }

    /**
     * Return the values observed for [field] across concrete re-executions.
     */
    fun getFlakyVariation(field: ResponseField): FieldVariation? {
        val values = flakyObservations
            .filter { it.source == FlakyObservationSource.RE_EXECUTION && it.execIndex != null }
            .sortedBy { it.execIndex }
            .mapNotNull { observation ->
                if (observation.differences.containsKey(field)) {
                    observation.execIndex!! to observation.differences[field]
                } else {
                    null
                }
            }
            .toMap()

        return if (values.isEmpty()) null else FieldVariation(field, values)
    }

    fun setFlakyErrorMessage(msg: String) = addFlakyDifference(ResponseField.ERROR_MESSAGE, msg)
    fun getFlakyErrorMessage() : String? = getFirstFlakyValue(ResponseField.ERROR_MESSAGE)

    fun setFlakyStatusCode(code: Int) = addFlakyDifference(ResponseField.STATUS_CODE, code.toString())
    fun getFlakyStatusCode() : Int? = getFirstFlakyValue(ResponseField.STATUS_CODE)?.toInt()

    fun setFlakyBody(body: String, execIndex : Int?=null) = addFlakyDifference(ResponseField.BODY, body, execIndex)
    fun getFlakyBody(execIndex : Int? = null) : String? =
        if (execIndex == null) getFirstFlakyValue(ResponseField.BODY)
        else getFlakyObservation(execIndex)?.differences?.get(ResponseField.BODY)

    fun getFlakyBodies() : List<String>? = getFlakyValues(ResponseField.BODY).filterNotNull().ifEmpty { null }
    fun containFlakyBody(flakyBody: String) : Boolean = getFlakyBodies()?.contains(flakyBody) ?: false

    /**
     * Merge all observed flaky body values into one representative body.
     *
     * For JSON objects and arrays, the original body is used as the baseline and each
     * observed flaky body contributes only the fields or elements that differ from it.
     * This allows different flaky fields observed in different executions to be handled
     * together when generating assertions.
     *
     * If there is no flaky body, the original body is returned. If the body is not
     * mergeable as JSON, the first observed flaky body is returned.
     */
    fun getMergedFlakyBody() : String{
        val originalBody = getBody()
        val flakyBodies = getFlakyBodies()

        if (flakyBodies.isNullOrEmpty()) {
            return originalBody ?: ""
        }

        if (originalBody == null) {
            return flakyBodies.first()
        }

        return try {
            val originalJson = mapper.readTree(originalBody)
            if (!originalJson.isObject && !originalJson.isArray) {
                return flakyBodies.first()
            }

            flakyBodies
                .map { mapper.readTree(it) }
                .fold(originalJson.deepCopy<JsonNode>()) { merged, observed ->
                    mergeJsonDiffFromOriginal(originalJson, observed, merged)
                }
                .let { mapper.writeValueAsString(it) }
        } catch (e: JsonProcessingException) {
            flakyBodies.first()
        } catch (e: IllegalStateException) {
            flakyBodies.first()
        }
    }

    private fun mergeJsonDiffFromOriginal(
        original: JsonNode,
        observed: JsonNode,
        merged: JsonNode
    ): JsonNode {
        if (original == observed) {
            return merged
        }

        if (original is ObjectNode && observed is ObjectNode && merged is ObjectNode) {
            val observedFieldNames = observed.fieldNames().asSequence().toSet()

            original.fieldNames().asSequence()
                .filter { !observedFieldNames.contains(it) }
                .forEach { merged.remove(it) }

            observed.fields().asSequence().forEach { (field, observedValue) ->
                val originalValue = original.get(field)
                if (originalValue == null) {
                    merged.set<JsonNode>(field, observedValue.deepCopy<JsonNode>())
                } else {
                    val mergedValue = merged.get(field) ?: originalValue.deepCopy<JsonNode>()
                    merged.set<JsonNode>(field, mergeJsonDiffFromOriginal(originalValue, observedValue, mergedValue))
                }
            }

            return merged
        }

        if (original is ArrayNode && observed is ArrayNode && merged is ArrayNode) {
            if (original.size() != observed.size()) {
                return observed.deepCopy<JsonNode>()
            }

            for (i in 0 until observed.size()) {
                merged.set(i, mergeJsonDiffFromOriginal(original[i], observed[i], merged[i]))
            }

            return merged
        }

        return observed.deepCopy<JsonNode>()
    }

    fun setFlakyBodyType(type: MediaType) = addFlakyDifference(ResponseField.BODY_TYPE, type.toString())
    fun getFlakyBodyType() : MediaType? = getFirstFlakyValue(ResponseField.BODY_TYPE)?.let { MediaType.valueOf(it) }

    fun setFlakiness(previous: HttpWsCallResult, execIndex: Int = 1) =
        recordFlakyObservation(previous, execIndex)

    private fun addFlakyDifference(field: ResponseField, value: String?, execIndex: Int? = null) {
        val index = execIndex ?: 1

        val existing = flakyObservations.find { it.execIndex == index }
        val differences = existing?.differences?.toMutableMap() ?: mutableMapOf()
        differences[field] = value

        flakyObservations.removeIf { it.source == FlakyObservationSource.RE_EXECUTION && it.execIndex == index }
        flakyObservations.add(FlakyObservation(FlakyObservationSource.RE_EXECUTION, index, differences))
    }

    private fun getFirstFlakyValue(field: ResponseField): String? =
        flakyObservations
            .sortedBy { it.execIndex }
            .firstNotNullOfOrNull { it.differences[field] }

    private data class ResponseFieldSpec(
        val field: ResponseField,
        val extract: (HttpWsCallResult) -> String?
    )

    private fun responseFieldExtractors(): List<ResponseFieldSpec> = listOf(
        ResponseFieldSpec(ResponseField.STATUS_CODE) { it.getStatusCode()?.toString() },
        ResponseFieldSpec(ResponseField.BODY) { it.getBody() },
        ResponseFieldSpec(ResponseField.BODY_TYPE) { it.getBodyType()?.toString() },
        ResponseFieldSpec(ResponseField.ERROR_MESSAGE) { it.getErrorMessage() }//,
//        ResponseFieldSpec(ResponseField.TOO_LARGE_BODY) { it.getTooLargeBody().toString() },
//        ResponseFieldSpec(ResponseField.INFINITE_LOOP) { it.getInfiniteLoop().toString() },
//        ResponseFieldSpec(ResponseField.TIMEDOUT) { it.getTimedout().toString() },
//        ResponseFieldSpec(ResponseField.TCP_PROBLEM) { it.getTcpProblem().toString() },
//        ResponseFieldSpec(ResponseField.INVALID_HTTP) { it.getInvalidHTTP().toString() },
//        ResponseFieldSpec(ResponseField.LOCATION) { it.getLocation() },
//        ResponseFieldSpec(ResponseField.ALLOW) { it.getAllow() },
//        ResponseFieldSpec(ResponseField.RESPONSE_TIME_MS) { it.getResponseTimeMs()?.toString() }
    )
}
