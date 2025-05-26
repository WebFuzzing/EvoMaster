package org.evomaster.core.problem.enterprise

import com.webfuzzing.commons.faults.FaultCategory

enum class ExperimentalFaultCategory(
    private val code: Int,
    private val descriptiveName: String,
    private val testCaseLabel: String
) : FaultCategory {


    //9xx for experimental, work-in-progress oracles

    HTTP_INVALID_PAYLOAD_SYNTAX(901, "Invalid Payload Syntax", "rejectedWithInvalidPayloadSyntax"),
    HTTP_INVALID_LOCATION(902, "Invalid Location HTTP Header", "returnsInvalidLocationHeader"),
    HTTP_NONWORKING_DELETE(903,"DELETE Method Does Not Work", "deleteDoesNotWork"),
    HTTP_REPEATED_CREATE_PUT(904, "Repeated PUT Creates Resource With 201", "repeatedCreatePut"),


    //3xx: GraphQL
    GQL_ERROR_FIELD(910, "Error Field", "returnedErrors"),

    //4xx: RPC
    // RPC internal error, eg thrift application internal error exception
    RPC_INTERNAL_ERROR(940, "Internal Error", "causesInternalError"),
    // RPC service error which is customized by user
    RPC_SERVICE_ERROR(941, "Service Error", "causesServiceError"),
    // exception for RPC
    RPC_DECLARED_EXCEPTION(942, "Declared Exception", "throwsExpectedException"),
    // unexpected exception for RPC
    RPC_UNEXPECTED_EXCEPTION(943,"Unexpected Exception", "throwsUnexpectedException"),
    // an RPC call which fails to achieve a successful business logic
    RPC_HANDLED_ERROR(944,"Business Logic Error", "failsToExecuteCall"),

    //5xx: Web Frontend
    WEB_BROKEN_LINK(950, "Broken Link", "returnsBrokenLink"),
    //6xx: mobile

    //8xx: security
    SECURITY_EXISTENCE_LEAKAGE(980, "Leakage Information Existence of Protected Resource", "allowsUnauthorizedAccessToProtectedResource"),
    SECURITY_NOT_RECOGNIZED_AUTHENTICATED(981, "Wrongly Not Recognized as Authenticated", "failedToAuthenticateWithValidCredentials"),
    SECURITY_FORBIDDEN_DELETE(982, "Forbidden Delete But Allowed Modifications", "forbidsDeleteButAllowsModifications"),
    SECURITY_FORBIDDEN_PUT(983, "Forbidden Replacement But Allowed Modifications", "forbidsReplacementButAllowsModifications"),
    SECURITY_FORBIDDEN_PATCH(984, "Forbidden Updates But Allowed Modifications", "forbidsUpdatesButAllowsModifications"),
    SECURITY_ALLOW_MODIFICATION_BY_ALL(985, "Resource Created By An User Can Be Modified By All Other Users", "createdResourceCanBeModifiedByEveryone")
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
}