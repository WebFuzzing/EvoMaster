package org.evomaster.core.problem.enterprise

import com.webfuzzing.commons.faults.FaultCategory

enum class ExperimentalFaultCategory(
    private val code: Int,
    private val descriptiveName: String,
    private val testCaseLabel: String,
    private val fullDescription: String,
) : FaultCategory {

    //9xx for experimental, work-in-progress oracles

    //security
    //Likely this one is not really viable
    //SECURITY_ALLOW_MODIFICATION_BY_ALL(985, "Resource Created By An User Can Be Modified By All Other Users", "createdResourceCanBeModifiedByEveryone",
    //  "TODO")
    HIDDEN_ACCESSIBLE_ENDPOINT(903, "Hidden Accessible Endpoint",
        "hiddenAccessible",
        "TODO"),


    HTTP_INVALID_PAYLOAD_SYNTAX(911, "Invalid Payload Syntax", "rejectedWithInvalidPayloadSyntax",
        "TODO"),
    HTTP_INVALID_LOCATION(912, "Invalid Location HTTP Header", "returnsInvalidLocationHeader",
        "TODO"),
    HTTP_NONWORKING_DELETE(913,"DELETE Method Does Not Work", "deleteDoesNotWork",
        "TODO"),
    HTTP_REPEATED_CREATE_PUT(914, "Repeated PUT Creates Resource With 201", "repeatedCreatePut",
        "TODO"),
    HTTP_SIDE_EFFECTS_FAILED_MODIFICATION(915, "A failed PUT or PATCH must not change the resource", "sideEffectsFailedModification",
        "TODO"),
    HTTP_PARTIAL_UPDATE_PUT(916, "The verb PUT makes a full replacement", "partialUpdatePut",
        "TODO"),
    HTTP_MISLEADING_CREATE_PUT(917, "PUT if creating, must get 201", "misleadingCreatePut",
        "TODO"),

    HTTP_STATUS_NO_NON_STANDARD_CODES(950, "no-non-standard-codes", "invalidStatusCode", "TODO"),
    HTTP_STATUS_NO_201_IF_DELETE(951, "no-201-if-delete", "201OnDelete",  "TODO"),
    HTTP_STATUS_NO_201_IF_GET(952, "no-201-if-get", "201OnGet",  "TODO"),
    HTTP_STATUS_NO_201_IF_PATCH(953, "no-201-if-patch", "201OnPatch",  "TODO"),
    HTTP_STATUS_NO_204_IF_CONTENT(954, "no-204-if-content", "204WhenContent",  "TODO"),
    HTTP_STATUS_NO_413_IF_NO_PAYLOAD(955, "no-413-if-no-payload", "413WhenNoPayload",  "TODO"),
    HTTP_STATUS_NO_415_IF_NO_PAYLOAD(956, "no-415-if-no-payload", "415WhenNoPayload",  "TODO"),
    HTTP_STATUS_NO_401_IF_NO_AUTH(957, "no-401-if-no-auth", "401WhenNoAuth",  "TODO"),
    HTTP_STATUS_NO_403_IF_NO_401(958, "no-403-if-no-401", "403WhenNo401",  "TODO"),
    HTTP_STATUS_HAS_406_IF_ACCEPT(959, "has-406-if-accept", "406WhenValid",  "TODO"),


    //3xx: GraphQL
    GQL_ERROR_FIELD(920, "Error Field", "returnedErrors",
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
    WEB_BROKEN_LINK(960, "Broken Link", "returnsBrokenLink",
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
