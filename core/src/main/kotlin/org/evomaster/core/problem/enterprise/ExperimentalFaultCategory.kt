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
        "Write operations that fail due to user errors should not leave side effects on the system." +
                " There should not be partial updates: either all are applied, or none."),
    HTTP_REPEATED_CREATE_PUT(902, "Repeated PUT Creates Resource With 201 Instead of Updating", "repeatedCreatePut",
        "A PUT operation can either update (e.g., 200 or 204) or create (201) a resource." +
                " If a resource is 201 created with a PUT, a second PUT should update the resource, and not be marked as recreated."),
    HTTP_MISLEADING_CREATE_PUT(903, "Misleading PUT 201 Creates When Resource Already Exists", "misleadingCreatePut",
        "A PUT operation can either update (e.g., 200 or 204) or create (201) a resource." +
                " If a resource already exists, than a PUT operation on it would update it, and not create it."),
    HTTP_PARTIAL_UPDATE_PUT(904, "The Verb PUT Must Make a Full Replacement", "partialUpdatePut",
        "A PUT operation is used to make a full replacement of a resource"),
    HTTP_NON_IDEMPOTENT_PUT(905, "PUT Implementation Must be Idempotent", "nonIdempotentPut",
        "A PUT operation is treated as idempotent. A write operation with a PUT that is not implemented as idempotent might have severe repercussions," +
                " as such operation could be automatically repeated any entity involved in the HTTP connection without any warning to the user."),
    HTTP_INVALID_MERGE_PATCH(906, "Invalid JSON Merge Patch", "invalidMergePatch",
        "A JSON Merge Path has a specific semantics, defining how values are modified based on the input payloads." +
                " Modifying entries not specified in the payload would be a clear implementation fault."),
    HTTP_INVALID_LOCATION(907, "Invalid Location HTTP Header", "returnsInvalidLocationHeader",
        "Even outside of 3xx redirections, the Location header can be used to specify for example where newly created resources can be accessed." +
                " However, if a Location value point to a path for which there is no valid operation (not necessarily a GET) in the API, then such value might" +
                " be likely wrong."),

    HTTP_INVALID_ALLOW(908, "Invalid Allow HTTP Header", "invalidAllow",
        "A returned Allow header specifies what operations (e.g., GET and PATCH) are available on a resource." +
                " For consistency, this needs to match what actually defined in the schema of the API, apart from special cases" +
                " such as HEAD and OPTIONS."),

    HTTP_TIMEOUT(909, "Request Timeout", "requestTimeout", "TODO"),

    HTTP_STATUS_NO_NON_STANDARD_CODES(950, "HTTP/REST-Design Violation: no-non-standard-codes", "invalidStatusCode",
        "HTTP status codes outside the range 100-599 are not valid."),
    HTTP_STATUS_NO_201_IF_DELETE(951, "HTTP/REST-Design Violation: no-201-if-delete", "201OnDelete",
        "A DELETE operation is meant to remove a resource, and so it should not states to 201 create one."),
    HTTP_STATUS_NO_201_IF_GET(952, "HTTP/REST-Design Violation: no-201-if-get", "201OnGet",
        "A GET operation is meant to retrieve a resource, and so it should not states to 201 create one."),
    HTTP_STATUS_NO_201_IF_PATCH(953, "HTTP/REST-Design Violation: no-201-if-patch", "201OnPatch",
        "A PATCH operation is meant to modify a resource, and so it should not states to 201 create one."),
    HTTP_STATUS_NO_204_IF_CONTENT(954, "HTTP/REST-Design Violation: no-204-if-content", "204WhenContent",
        "If a response contains a payload, it should not state it 204 contains none."),
    HTTP_STATUS_NO_413_IF_NO_PAYLOAD(955, "HTTP/REST-Design Violation: no-413-if-no-payload", "413WhenNoPayload",
        "Cannot state a payload is too large if there is no payload."),
    HTTP_STATUS_NO_415_IF_NO_PAYLOAD(956, "HTTP/REST-Design Violation: no-415-if-no-payload", "415WhenNoPayload",
        "Cannot state a payload is of the wrong type if there is no payload."),
    HTTP_STATUS_NO_304_IF_NO_GET_OR_HEAD(960, "HTTP/REST-Design Violation: no-304-if-no-get-or-head", "304OnWrongVerb",
        "A 304 response is not valid if the request was not either a GET or a HEAD."),
    HTTP_STATUS_NO_401_IF_NO_WWW_AUTHENTICATE(961, "HTTP/REST-Design Violation: no-401-if-no-authenticate", "401MissingWwwAuthenticate",
        "If an API responds with a 401 non-authenticated error, such response MUST contain a www-authenticate header, with the needed information." +
                " In HTTP, this is not optional."),
    HTTP_STATUS_NO_405_IF_NO_ALLOW(962, "HTTP/REST-Design Violation: no-405-if-no-allow", "405MissingAllow",
        "A 405 Not Allowed response must contain an Allow header specifying what is allowed."),
    HTTP_STATUS_NO_205_IF_CONTENT(964,"HTTP/REST-Design Violation: no-205-if-content","205WhenContent",
        "If a response contains a payload, it should not return a 205, as that requires no payload."),
    HTTP_STATUS_NO_426_IF_NO_UPGRADE(965,"HTTP/REST-Design Violation: no-426-if-no-upgrade","426MissingUpgrade",
        "A 426 response must contain an Upgrade header with the needed information."),


    HTTP_STATUS_NO_401_IF_NO_AUTH(957, "HTTP/REST-Design Violation: no-401-if-no-auth", "401WhenNoAuth",
        "Should not return a 401 non-authenticated if there is no authentication in the definition of the API (or conversely, authentication definition" +
                " is wrongly missing)."),
    HTTP_STATUS_NO_403_IF_NO_401(958, "HTTP/REST-Design Violation: no-403-if-no-401", "403WhenNo401",
        "Should not return a 403 non-authorized if there is no 401 non-authenticated in the definition of the API (or conversely, such definition" +
                " is wrongly missing)."),
    HTTP_STATUS_HAS_406_IF_ACCEPT(959, "HTTP/REST-Design Violation: has-406-if-accept", "406WhenValid",
        "If a valid payload is sent based on what declared in the schema, it should not happen that the API responds with a 406" +
                " non-valid payload type."),
    HTTP_STATUS_NO_501_IF_IMPLEMENTED(963, "HTTP/REST-Design Violation: no-501-if-implemented", "501OnDeclaredEndpoint",
        "If a schema defines an endpoint, then a call on it should not return a 501 Non-Implemented."),


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
