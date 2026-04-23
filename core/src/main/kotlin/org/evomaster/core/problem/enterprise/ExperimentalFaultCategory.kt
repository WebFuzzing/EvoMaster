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
    IGNORE_ANONYMOUS(900, "A Protected Resource Is Accessible Without Providing Any Authentication",
        "ignoreAnonymous",
        "TODO"),
    ANONYMOUS_MODIFICATIONS(901, "Anonymous Modifications",
        "anonymousModifications",
        "TODO"),
    LEAKED_STACK_TRACES(902, "Leaked Stack Trace",
        "leakedStackTrace",
        "TODO"),
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
    WEB_BROKEN_LINK(950, "Broken Link", "returnsBrokenLink",
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
