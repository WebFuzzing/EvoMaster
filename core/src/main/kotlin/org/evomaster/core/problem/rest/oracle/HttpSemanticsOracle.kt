package org.evomaster.core.problem.rest.oracle

import io.swagger.v3.oas.models.media.Schema
import org.evomaster.core.output.formatter.OutputFormatter
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestCallResult
import org.evomaster.core.problem.rest.data.RestIndividual
import org.evomaster.core.problem.rest.param.BodyParam
import org.evomaster.core.problem.rest.schema.RestSchema
import org.evomaster.core.problem.rest.schema.SchemaUtils
import org.evomaster.core.problem.rest.StatusGroup
import org.evomaster.core.search.action.ActionResult
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.gene.wrapper.OptionalGene

object HttpSemanticsOracle {



    fun hasRepeatedCreatePut(individual: RestIndividual,
                             actionResults: List<ActionResult>
    ): Boolean{

        if(individual.size() < 2){
            return false
        }
        val actions = individual.seeMainExecutableActions()
        val first = actions[actions.size - 2]  // PUT 201
        val second = actions[actions.size - 1] // PUT 201

        //both using PUT
        if(first.verb != HttpVerb.PUT || second.verb != HttpVerb.PUT){
            return false
        }

        //on same resource
        if(! first.usingSameResolvedPath(second)){
            return false
        }

        //with same auth
        if(first.auth.isDifferentFrom(second.auth)){
            /*
                this might require some explanation. What if instead of a parametric endpoint
                /x/{id}
                we have a static
                /x
                where different resources are based on auth info?
                in this latter case, 2 PUTs with 201 on /x could be fine if using different auths
             */
            return false
        }

        val res0 = actionResults.find { it.sourceLocalId == first.getLocalId() } as RestCallResult?
            ?: return false
        val res1 = actionResults.find { it.sourceLocalId == second.getLocalId() } as RestCallResult?
            ?: return false

        //both must be 201 CREATE
        if(res0.getStatusCode() != 201 || res1.getStatusCode() != 201){
            return false
        }

        return true
    }


    class NonWorkingDeleteResult(
        val checkingDelete: Boolean = false,
        val nonWorking: Boolean = false,
        val name: String = "",
        val index: Int = -1
    )

    fun hasNonWorkingDelete(individual: RestIndividual,
                            actionResults: List<ActionResult>
    ) : NonWorkingDeleteResult {

        if(individual.size() < 3){
            return NonWorkingDeleteResult()
        }

        val actions = individual.seeMainExecutableActions()

        val before = actions[actions.size - 3]  // GET 2xx
        val delete = actions[actions.size - 2]  // DELETE 2xx
        val after = actions[actions.size - 1]   // GET 2xx

        //check verbs
        if(before.verb != HttpVerb.GET || delete.verb != HttpVerb.DELETE ||  after.verb != HttpVerb.GET) {
            return NonWorkingDeleteResult()
        }

        //check path resolution
        if(!before.usingSameResolvedPath(delete) || !after.usingSameResolvedPath(delete)) {
            return NonWorkingDeleteResult()
        }

        val res0 = actionResults.find { it.sourceLocalId == before.getLocalId() } as RestCallResult?
            ?: return NonWorkingDeleteResult()
        val res1 = actionResults.find { it.sourceLocalId == delete.getLocalId() } as RestCallResult?
            ?: return NonWorkingDeleteResult()
        val res2 = actionResults.find { it.sourceLocalId == after.getLocalId() } as RestCallResult?
            ?: return NonWorkingDeleteResult()

        // GET followed by DELETE, both 2xx, so working fine
        val checkingDelete = StatusGroup.G_2xx.allInGroup(res0.getStatusCode(), res1.getStatusCode())
        // all fine, but repeated GET after DELETE wrongly returns 2xx with data, meaning DELETE didn't delete
        val nonWorking: Boolean = checkingDelete && StatusGroup.G_2xx.allInGroup(res2.getStatusCode())
                && !res2.getBody().isNullOrEmpty()

        return NonWorkingDeleteResult(checkingDelete, nonWorking, delete.getName(), actions.size - 2)
    }

    fun hasSideEffectFailedModification(individual: RestIndividual,
                             actionResults: List<ActionResult>
    ): Boolean{

        if(individual.size() < 3){
            return false
        }

        val actions = individual.seeMainExecutableActions()

        val before  = actions[actions.size - 3]  // GET (before state)
        val modify  = actions[actions.size - 2]  // PUT or PATCH (failed modification)
        val after   = actions[actions.size - 1]  // GET (after state)

        // check verbs: GET, PUT|PATCH, GET
        if(before.verb != HttpVerb.GET) {
            return false
        }
        if(modify.verb != HttpVerb.PUT && modify.verb != HttpVerb.PATCH) {
            return false
        }
        if(after.verb != HttpVerb.GET) {
            return false
        }

        // all three must be on the same resolved path
        if(!before.usingSameResolvedPath(modify) || !after.usingSameResolvedPath(modify)) {
            return false
        }

        // the two GETs must use the same auth so the state comparison is meaningful.
        if(before.auth.isDifferentFrom(after.auth)) {
            return false
        }

        val resBefore = actionResults.find { it.sourceLocalId == before.getLocalId() } as RestCallResult?
            ?: return false
        val resModify = actionResults.find { it.sourceLocalId == modify.getLocalId() } as RestCallResult?
            ?: return false
        val resAfter = actionResults.find { it.sourceLocalId == after.getLocalId() } as RestCallResult?
            ?: return false

        // before GET must be 2xx
        if(!StatusGroup.G_2xx.isInGroup(resBefore.getStatusCode())) {
            return false
        }

        // PUT/PATCH must have failed with 4xx
        if(!StatusGroup.G_4xx.isInGroup(resModify.getStatusCode())) {
            return false
        }

        // this oracle only supports JSON, XML, and form-urlencoded request bodies;
        // other content types (e.g. text/plain, multipart, binary) are not handled
        val bodyParam = modify.parameters.find { it is BodyParam } as BodyParam?
        if (bodyParam != null && !bodyParam.isJson() && !bodyParam.isXml() && !bodyParam.isForm()) {
            return false
        }

        // after GET must be 2xx
        if(!StatusGroup.G_2xx.isInGroup(resAfter.getStatusCode())) {
            return false
        }

        val bodyBefore = resBefore.getBody()
        val bodyModify = extractRequestBody(modify)
        val bodyAfter = resAfter.getBody()

        // if both are null/empty, no side-effect detected
        if(bodyBefore.isNullOrEmpty() && bodyAfter.isNullOrEmpty()) {
            return false
        }

        // extract the field names sent in the PUT/PATCH request body
        val modifiedFieldNames = extractModifiedFieldNames(modify)

        // if we can identify specific fields, compare only those to avoid false positives from timestamps etc.
        if(modifiedFieldNames.isNotEmpty()
            && !bodyBefore.isNullOrEmpty()
            && !bodyAfter.isNullOrEmpty()
            && !bodyModify.isNullOrEmpty()) {
            return hasChangedModifiedFields(bodyBefore, bodyAfter, bodyModify, modifiedFieldNames, bodyParam)
        }

        return false
    }

    private fun extractRequestBody(modify: RestCallAction): String? {
        val bodyParam = modify.parameters.find { it is BodyParam } as BodyParam?
            ?: return null
        val mode = when {
            bodyParam.isJson() -> GeneUtils.EscapeMode.JSON
            bodyParam.isXml()  -> GeneUtils.EscapeMode.XML
            bodyParam.isForm() -> GeneUtils.EscapeMode.X_WWW_FORM_URLENCODED
            else               -> null
        }
        return bodyParam.getValueAsPrintableString(mode = mode)
    }

    /**
     * Checks the special K==404 side-effect pattern:
     *
     *   GET       /path  → 404   (resource does not exist before the call)
     *   PUT|PATCH /path  → 404   (failed modification - resource still not found)
     *   GET       /path  → ???   (should STILL be 404; anything else is a side-effect)
     */
    fun hasSideEffectIn404Modification(
        individual: RestIndividual,
        actionResults: List<ActionResult>
    ): Boolean {

        if(individual.size() < 3) return false

        val actions = individual.seeMainExecutableActions()
        val before = actions[actions.size - 3]  // GET  (should be 404)
        val modify = actions[actions.size - 2]  // PUT or PATCH (should be 404)
        val after  = actions[actions.size - 1]  // GET  (oracle target)

        if(before.verb != HttpVerb.GET) return false
        if(modify.verb != HttpVerb.PUT && modify.verb != HttpVerb.PATCH) return false
        if(after.verb  != HttpVerb.GET) return false

        if(!before.usingSameResolvedPath(modify) || !after.usingSameResolvedPath(modify)) return false

        val resBefore = actionResults.find { it.sourceLocalId == before.getLocalId() } as RestCallResult?
            ?: return false
        val resModify = actionResults.find { it.sourceLocalId == modify.getLocalId() } as RestCallResult?
            ?: return false
        val resAfter  = actionResults.find { it.sourceLocalId == after.getLocalId()  } as RestCallResult?
            ?: return false

        if(resBefore.getStatusCode() != 404) return false
        if(resModify.getStatusCode() != 404) return false

        return resAfter.getStatusCode() != 404
    }

    /**
     * Checks the PUT full-replacement oracle.
     *
     * Sequence:
     *   PUT /path  -> 2xx   (full replacement with body B)
     *   GET /path  -> 2xx   (must reflect B exactly, excluding fields outside the PUT schema)
     *
     * Two independent checks are performed:
     *
     *  1. Sent fields (fields actually included in the PUT body) must come back
     *     in the GET with the same value. Differences are bugs (partial update).
     *
     *  2. Wiped fields (fields that are part of the PUT schema but were not
     *     included in this request) must be absent or null in the GET. PUT is a
     *     full replacement, so the server should have cleared them. Any leftover
     *     value is also a partial-update bug.
     *
     * Fields that exist in the GET response schema but are NOT in the PUT schema
     * (e.g. server-managed `id`, `createdAt`) are ignored: PUT cannot control them.

     */
    fun hasMismatchedPutResponse(
        individual: RestIndividual,
        actionResults: List<ActionResult>,
        schema: RestSchema? = null
    ): Boolean {

        if (individual.size() < 2) return false

        val actions = individual.seeMainExecutableActions()
        val put = actions[actions.size - 2]
        val get = actions[actions.size - 1]

        if (put.verb != HttpVerb.PUT) return false
        if (get.verb != HttpVerb.GET) return false

        if (!put.usingSameResolvedPath(get)) return false

        if (put.auth.isDifferentFrom(get.auth)) return false

        val resPut = actionResults.find { it.sourceLocalId == put.getLocalId() } as RestCallResult?
            ?: return false
        val resGet = actionResults.find { it.sourceLocalId == get.getLocalId() } as RestCallResult?
            ?: return false

        if (!StatusGroup.G_2xx.isInGroup(resPut.getStatusCode())) return false
        if (!StatusGroup.G_2xx.isInGroup(resGet.getStatusCode())) return false

        val bodyParam = put.parameters.find { it is BodyParam } as BodyParam?
        if (bodyParam != null && !bodyParam.isJson() && !bodyParam.isXml() && !bodyParam.isForm()) {
            return false
        }

        val putBody = extractRequestBody(put)
        val getBody = resGet.getBody()

        if (putBody.isNullOrEmpty() && !getBody.isNullOrEmpty()) return true

        // if putBody is not empty but getBody is.
        if (!putBody.isNullOrEmpty() && getBody.isNullOrEmpty()) return true

        if (putBody.isNullOrEmpty() || getBody.isNullOrEmpty()) return false

        val sentFields = extractSentFieldNames(put)
        val allPutSchemaFields = extractModifiedFieldNames(put)
        if (sentFields.isEmpty() && allPutSchemaFields.isEmpty()) return false

        // Wiped = in PUT schema but not sent. PUT semantics say the server must
        // clear these. We only check wiped fields the GET schema actually exposes,
        // otherwise write-only fields (e.g. passwords) would cause false positives.
        val wipedCandidates = allPutSchemaFields - sentFields
        val wipedFields = if (schema != null && wipedCandidates.isNotEmpty()) {
            val getSchemaFields = extractGetResponseSchemaFields(schema, get)
            if (getSchemaFields.isEmpty()) emptySet() else wipedCandidates intersect getSchemaFields
        } else {
            emptySet()
        }

        return hasMismatchedPutFields(putBody, getBody, sentFields, wipedFields, bodyParam)
    }

    /**
     * Dispatches the field-level checks based on the request body content type.
     *
     * @param sentFields  fields whose values must match between PUT and GET
     * @param wipedFields fields that must be absent (or null) in the GET response
     */
    internal fun hasMismatchedPutFields(
        putBody: String,
        getBody: String,
        sentFields: Set<String>,
        wipedFields: Set<String>,
        bodyParam: BodyParam? = null
    ): Boolean {
        return when {
            bodyParam == null || bodyParam.isJson() ->
                hasMismatchedPutFieldsStructured(OutputFormatter.JSON_FORMATTER, putBody, getBody, sentFields, wipedFields)
            bodyParam.isXml() ->
                hasMismatchedPutFieldsStructured(OutputFormatter.XML_FORMATTER, putBody, getBody, sentFields, wipedFields)
            bodyParam.isForm() ->
                hasMismatchedPutFieldsForm(putBody, getBody, sentFields, wipedFields)
            else -> false
        }
    }

    private fun hasMismatchedPutFieldsStructured(
        formatter: OutputFormatter,
        putBody: String,
        getBody: String,
        sentFields: Set<String>,
        wipedFields: Set<String>
    ): Boolean {

        // sent fields: PUT value must equal GET value
        if (sentFields.isNotEmpty()) {
            val fieldsPut = formatter.readFields(putBody, sentFields) ?: return false
            if (fieldsPut.isNotEmpty()) {
                val fieldsGet = formatter.readFields(getBody, fieldsPut.keys.toSet()) ?: return false
                for ((field, valuePut) in fieldsPut) {
                    val valueGet = fieldsGet[field] ?: return true  // sent but missing in GET
                    if (valuePut != valueGet) return true
                }
            }
        }

        // wiped fields: must be absent or null in GET
        if (wipedFields.isNotEmpty()) {
            val getWiped = formatter.readFields(getBody, wipedFields) ?: return false
            for (field in wipedFields) {
                if (isWipedFieldStillPresent(getWiped[field])) return true
            }
        }

        return false
    }

    /**
     * Handles the case where the PUT request body is form-encoded.
     * The GET response format is auto-detected by trying JSON then XML.
     */
    private fun hasMismatchedPutFieldsForm(
        putBody: String,
        getBody: String,
        sentFields: Set<String>,
        wipedFields: Set<String>
    ): Boolean {
        val formFields = parseFormBody(putBody)
        if (formFields.isEmpty() && sentFields.isNotEmpty()) return false

        for (formatter in listOf(OutputFormatter.JSON_FORMATTER, OutputFormatter.XML_FORMATTER)) {
            val fieldsGetSent = if (sentFields.isEmpty()) emptyMap()
                                else formatter.readFields(getBody, sentFields) ?: continue

            // sent fields
            for (field in sentFields) {
                val valuePut = formFields[field] ?: continue
                val valueGet = fieldsGetSent[field] ?: return true
                if (valuePut != valueGet) return true
            }

            // wiped fields
            if (wipedFields.isNotEmpty()) {
                val getWiped = formatter.readFields(getBody, wipedFields) ?: return false
                for (field in wipedFields) {
                    if (isWipedFieldStillPresent(getWiped[field])) return true
                }
            }
            return false
        }
        return false
    }

    /**
     * A wiped field is present only if the GET response returns a non-null, non-empty value.
     */
    private fun isWipedFieldStillPresent(value: String?): Boolean {
        if (value == null) return false
        if (value.isEmpty()) return false
        if (value == "null") return false
        return true
    }

    /**
     * Extract field names from the PUT/PATCH request body.
     * These are the fields that the client attempted to modify.
     */
    private fun extractModifiedFieldNames(modify: RestCallAction): Set<String> {

        val objectGene = extractBodyObjectGene(modify) ?: return emptySet()

        return objectGene.fields.map { it.name }.toSet()
    }

    // Extract only the field names that were actually sent in the request body.
    private fun extractSentFieldNames(modify: RestCallAction): Set<String> {

        val objectGene = extractBodyObjectGene(modify) ?: return emptySet()

        return objectGene.fields
            .filter { f -> (f as? OptionalGene)?.isActive ?: true }
            .map { it.name }
            .toSet()
    }

    private fun extractBodyObjectGene(modify: RestCallAction): ObjectGene? {
        val bodyParam = modify.parameters.find { it is BodyParam } as BodyParam?
            ?: return null

        val gene = bodyParam.primaryGene()
        return gene.getWrappedGene(ObjectGene::class.java) as ObjectGene?
            ?: if (gene is ObjectGene) gene else null
    }

    /**
     * Compares only the fields that were sent in the PUT/PATCH request.
     * Returns true if any of those fields changed between the before and after GET responses.
     *
     * Dispatches to a format-specific comparison based on [bodyParam] content type:
     * - JSON         : field-by-field comparison via [OutputFormatter.JSON_FORMATTER]
     * - XML          : field-by-field comparison via XML DOM parsing
     * - form-encoded : GET responses parsed as JSON or XML, request parsed as key=value pairs
     * - other        : returns false (incl. text/plain, which may cause too many false positives)
     *
     * NOTE: Does not support operation-based payloads such as JSON Patch (RFC 6902).
     */
    internal fun hasChangedModifiedFields(
        bodyBefore: String,
        bodyAfter: String,
        bodyModify: String,
        fieldNames: Set<String>,
        bodyParam: BodyParam? = null
    ): Boolean {
        return when {
            bodyParam == null || bodyParam.isJson() ->
                hasChangedModifiedFieldsStructured(OutputFormatter.JSON_FORMATTER, bodyBefore, bodyAfter, bodyModify, fieldNames)
            bodyParam.isXml() ->
                hasChangedModifiedFieldsStructured(OutputFormatter.XML_FORMATTER, bodyBefore, bodyAfter, bodyModify, fieldNames)
            bodyParam.isForm() ->
                hasChangedModifiedFieldsForm(bodyBefore, bodyAfter, bodyModify, fieldNames)
            else -> false
        }
    }

    /**
     * Generic field-level comparison for structured formats (JSON, XML).
     * Uses [formatter]'s [OutputFormatter.readFields] to extract field values from all three bodies.
     */
    private fun hasChangedModifiedFieldsStructured(
        formatter: OutputFormatter,
        bodyBefore: String,
        bodyAfter: String,
        bodyModify: String,
        fieldNames: Set<String>
    ): Boolean {
        val fieldsBefore = formatter.readFields(bodyBefore, fieldNames) ?: return false
        val fieldsAfter  = formatter.readFields(bodyAfter,  fieldNames) ?: return false
        val fieldsModify = formatter.readFields(bodyModify, fieldNames) ?: return false

        for (field in fieldNames) {
            val valueBefore = fieldsBefore[field] ?: continue
            val valueAfter  = fieldsAfter[field]

            // field existed before but disappeared after the failed modification
            if (valueAfter == null) return true

            val valueModify = fieldsModify[field] ?: continue

            // checking valueModify==valueAfter (not just valueAfter!=valueBefore) is done to
            // deal with possible flakiness issues (e.g. timestamps changing between the two GETs)
            if (valueBefore != valueAfter && valueModify == valueAfter) return true
        }
        return false
    }

    /**
     * Handles the case where the PUT/PATCH request body is form-encoded.
     * GET responses (bodyBefore / bodyAfter) can be JSON or XML; the format is auto-detected
     * by trying each formatter in order and using the first one that parses both responses.
     * Values are compared as strings since form-encoded values are always strings.
     */
    private fun hasChangedModifiedFieldsForm(
        bodyBefore: String,
        bodyAfter: String,
        bodyModify: String,
        fieldNames: Set<String>
    ): Boolean {
        val formFields = parseFormBody(bodyModify)
        if (formFields.isEmpty()) return false

        for (formatter in listOf(OutputFormatter.JSON_FORMATTER, OutputFormatter.XML_FORMATTER)) {
            val fieldsBefore = formatter.readFields(bodyBefore, fieldNames) ?: continue
            val fieldsAfter  = formatter.readFields(bodyAfter,  fieldNames) ?: continue

            for (field in fieldNames) {
                val valueBefore = fieldsBefore[field] ?: continue
                val valueAfter  = fieldsAfter[field]

                // field existed before but disappeared after the failed modification
                if (valueAfter == null) return true

                val valueModify = formFields[field]   ?: continue

                // checking valueModify==valueAfter (not just valueAfter!=valueBefore) is done to
                // deal with possible flakiness issues (e.g. timestamps changing between the two GETs)
                if (valueBefore != valueAfter && valueModify == valueAfter) return true
            }
            return false
        }
        return false
    }

    /**
     * Returns the property names from the GET 2xx response schema in the OpenAPI spec.
     * Empty set if unresolvable, which makes callers skip wiped-field checks.
     */
    internal fun extractGetResponseSchemaFields(
        schema: RestSchema,
        get: RestCallAction
    ): Set<String> {

        val openAPI = schema.main.schemaParsed
        val pathItem = openAPI.paths?.get(get.path.toString()) ?: return emptySet()
        val op = pathItem.get ?: return emptySet()

        // pick the first 2xx response, falling back to "default"
        val response = op.responses?.entries
            ?.firstOrNull { it.key.length == 3 && it.key.startsWith("2") }?.value
            ?: op.responses?.get("default")
            ?: return emptySet()

        val mediaType = response.content?.values?.firstOrNull() ?: return emptySet()
        val rawSchema = mediaType.schema ?: return emptySet()
        val resolved = rawSchema.`$ref`?.let {
            SchemaUtils.getReferenceSchema(schema, schema.main, it, mutableListOf())
        } ?: rawSchema

        return resolved.properties?.keys?.toSet() ?: emptySet()
    }

    private fun parseFormBody(body: String): Map<String, String> {
        return body.split("&").mapNotNull { pair ->
            val parts = pair.split("=", limit = 2)
            if (parts.size == 2) {
                try {
                    java.net.URLDecoder.decode(parts[0], "UTF-8") to
                        java.net.URLDecoder.decode(parts[1], "UTF-8")
                } catch (e: Exception) { null }
            } else null
        }.toMap()
    }
}
