package org.evomaster.core.problem.enterprise

import com.webfuzzing.commons.faults.FaultCategory

enum class ExperimentalFaultCategory(
    private val code: Int,
    private val descriptiveName: String,
    private val testCaseLabel: String,
    private val fullDescription: String,
) : FaultCategory {

    //9xx for experimental, work-in-progress oracles

    HTTP_NONWORKING_DELETE(900,"Resource Still Accessible After Successful DELETE", "deleteDoesNotWork",
        "If a resource is deleted, and the API responds that such request was successful, then such" +
                " resource should no longer being available." +
                " New requests to access it should fail." +
                " Otherwise, if it is still possible to access the resource, then it was not really deleted." +
                " Then, as such, it means that the delete operation is faulty."),
    HTTP_SIDE_EFFECTS_FAILED_MODIFICATION(901, "A Failed PUT or PATCH Must Not Change The Resource", "sideEffectsFailedModification",
        "TODO"),
    HTTP_REPEATED_CREATE_PUT(902, "Repeated PUT Creates Resource With 201 Instead of Updating", "repeatedCreatePut",
        "TODO"),
    HTTP_MISLEADING_CREATE_PUT(903, "Misleading PUT 201 Creates When Resource Already Exists", "misleadingCreatePut",
        "TODO"),
    HTTP_PARTIAL_UPDATE_PUT(904, "The Verb PUT Must Make a Full Replacement", "partialUpdatePut",
        "TODO"),
    HTTP_NON_IDEMPOTENT_PUT(905, "PUT Implementation Must be Idempotent", "nonIdempotentPut",
        "TODO"),
    HTTP_INVALID_MERGE_PATCH(906, "Invalid JSON Merge Patch", "invalidMergePatch",
        "TODO"),
    HTTP_INVALID_LOCATION(907, "Invalid Location HTTP Header", "returnsInvalidLocationHeader",
        "TODO"),
    HTTP_INVALID_ALLOW(908, "Invalid Allow HTTP Header", "invalidAllow",
        "TODO"),
    HTTP_TIMEOUT(909, "Request Timeout", "requestTimeout", "TODO"),

    HTTP_STATUS_NO_NON_STANDARD_CODES(950, "HTTP/REST-Design Violation: no-non-standard-codes", "invalidStatusCode", "TODO"),
    HTTP_STATUS_NO_201_IF_DELETE(951, "HTTP/REST-Design Violation: no-201-if-delete", "201OnDelete",  "TODO"),
    HTTP_STATUS_NO_201_IF_GET(952, "HTTP/REST-Design Violation: no-201-if-get", "201OnGet",  "TODO"),
    HTTP_STATUS_NO_201_IF_PATCH(953, "HTTP/REST-Design Violation: no-201-if-patch", "201OnPatch",  "TODO"),
    HTTP_STATUS_NO_204_IF_CONTENT(954, "HTTP/REST-Design Violation: no-204-if-content", "204WhenContent",  "TODO"),
    HTTP_STATUS_NO_413_IF_NO_PAYLOAD(955, "HTTP/REST-Design Violation: no-413-if-no-payload", "413WhenNoPayload",  "TODO"),
    HTTP_STATUS_NO_415_IF_NO_PAYLOAD(956, "HTTP/REST-Design Violation: no-415-if-no-payload", "415WhenNoPayload",  "TODO"),
    HTTP_STATUS_NO_401_IF_NO_AUTH(957, "HTTP/REST-Design Violation: no-401-if-no-auth", "401WhenNoAuth",  "TODO"),
    HTTP_STATUS_NO_403_IF_NO_401(958, "HTTP/REST-Design Violation: no-403-if-no-401", "403WhenNo401",  "TODO"),
    HTTP_STATUS_HAS_406_IF_ACCEPT(959, "HTTP/REST-Design Violation: has-406-if-accept", "406WhenValid",  "TODO"),
    HTTP_STATUS_NO_304_IF_NO_GET_OR_HEAD(960, "HTTP/REST-Design Violation: no-304-if-no-get-or-head", "304OnWrongVerb", "TODO"),
    HTTP_STATUS_NO_401_IF_NO_WWW_AUTHENTICATE(961, "HTTP/REST-Design Violation: no-401-if-no-authenticate", "401MissingWwwAuthenticate", "TODO"),
    HTTP_STATUS_NO_405_IF_NO_ALLOW(962, "HTTP/REST-Design Violation: no-405-if-no-allow", "405MissingAllow", "TODO"),
    HTTP_STATUS_NO_501_IF_IMPLEMENTED(963, "HTTP/REST-Design Violation: no-501-if-implemented", "501OnDeclaredEndpoint", "TODO"),
    HTTP_STATUS_NO_205_IF_CONTENT(964,"HTTP/REST-Design Violation: no-205-if-content","205WhenContent", "TODO"),
    HTTP_STATUS_NO_426_IF_NO_UPGRADE(965,"HTTP/REST-Design Violation: no-426-if-no-upgrade","426MissingUpgrade", "TODO"),


    /*
     TODO Is this one still relevant? or subsumed by SCHEMA_INVALID_RESPONSE?
     old comment was:
     syntactically invalid response (eg, non-quoted text when expecting JSON. this happens in pet-clinic for example)
  */
    HTTP_INVALID_PAYLOAD_SYNTAX(929, "Invalid Payload Syntax", "rejectedWithInvalidPayloadSyntax",
        "TODO"),

    //3xx: GraphQL
    GQL_ERROR_FIELD(930, "Error Field", "returnedErrors",
        "TODO"),

    //4xx: RPC
    // RPC internal error, eg thrift application internal error exception
    RPC_INTERNAL_ERROR(940, "Internal Error", "causesInternalError",
        "TODO"),
    // RPC service error which is customized by user
    RPC_SERVICE_ERROR(941, "Service Error", "causesServiceError",
        "TODO"),
    // exception for RPC
    RPC_DECLARED_EXCEPTION(942, "Declared Exception", "throwsExpectedException",
        "TODO"),
    // unexpected exception for RPC
    RPC_UNEXPECTED_EXCEPTION(943,"Unexpected Exception", "throwsUnexpectedException",
        "TODO"),
    // an RPC call which fails to achieve a successful business logic
    RPC_HANDLED_ERROR(944,"Business Logic Error", "failsToExecuteCall",
        "TODO"),

    //5xx: Web Frontend
    WEB_BROKEN_LINK(980, "Broken Link", "returnsBrokenLink",
        "TODO"),
    //6xx: mobile

    ;

    override fun getCode(): Int {
        return code
    }

    override fun getDescriptiveName(): String {
        return descriptiveName
    }

    override fun getTestCaseLabel(): String {
        return testCaseLabel
    }

    override fun getFullDescription(): String {
        return fullDescription
    }
}
