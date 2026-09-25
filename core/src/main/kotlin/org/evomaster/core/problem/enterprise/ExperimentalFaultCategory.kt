package org.evomaster.core.problem.enterprise

import com.webfuzzing.commons.faults.FaultCategory

enum class ExperimentalFaultCategory(
    private val code: Int,
    private val descriptiveName: String,
    private val testCaseLabel: String,
    private val fullDescription: String,
) : FaultCategory {

    //9xx for experimental, work-in-progress oracles


    //Implemented
    HTTP_TIMEOUT(960, "Request Timeout", "requestTimeout", "TODO"),


    /*
     TODO Is this one still relevant? or subsumed by SCHEMA_INVALID_RESPONSE?
     old comment was:
     syntactically invalid response (eg, non-quoted text when expecting JSON. this happens in pet-clinic for example)
  */
    HTTP_INVALID_PAYLOAD_SYNTAX(961, "Invalid Payload Syntax", "rejectedWithInvalidPayloadSyntax",
        "TODO"),


    //RPC
    // RPC internal error, eg thrift application internal error exception
    RPC_INTERNAL_ERROR(970, "Internal Error", "causesInternalError",
        "TODO"),
    // RPC service error which is customized by user
    RPC_SERVICE_ERROR(971, "Service Error", "causesServiceError",
        "TODO"),
    // exception for RPC
    RPC_DECLARED_EXCEPTION(972, "Declared Exception", "throwsExpectedException",
        "TODO"),
    // unexpected exception for RPC
    RPC_UNEXPECTED_EXCEPTION(973,"Unexpected Exception", "throwsUnexpectedException",
        "TODO"),
    // an RPC call which fails to achieve a successful business logic
    RPC_HANDLED_ERROR(974,"Business Logic Error", "failsToExecuteCall",
        "TODO"),

    //Web Frontend
    WEB_BROKEN_LINK(980, "Broken Link", "returnsBrokenLink",
        "TODO"),

    //mobile

    //GraphQL
    GQL_ERROR_FIELD(990, "Error Field", "returnedErrors",
        "TODO"),

    //9xx: MCP
    MCP_BROKEN_RESOURCE(981, "Broken Advertised Resource", "brokenAdvertisedResource",
        "A read attempt on a resource returned JSON-RPC 'resource not found' error (code -32002)"),
    MCP_INTERNAL_ERROR(982, "MCP Tool Internal Error", "causesInternalErrorMCP",
        "A tools/call request resulted in a JSON-RPC protocol-level error with code -32603 (Internal error)"),

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
