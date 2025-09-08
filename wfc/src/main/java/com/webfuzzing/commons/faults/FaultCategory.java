package com.webfuzzing.commons.faults;

import java.util.Objects;

public enum FaultCategory {

    /*
        TODO
        code label are still up to discussion and re-arrangement...
     */

//    test_1_GET_on_items_


    //1xx: HTTP
    HTTP_STATUS_500(100, "HTTP Status 500", "causes500_internalServerError"),
    HTTP_INVALID_PAYLOAD_SYNTAX(101, "Invalid Payload Syntax", "rejectedWithInvalidPayloadSyntax"),
    HTTP_INVALID_LOCATION(102, "Invalid Location HTTP Header", "returnsInvalidLocationHeader"),
    HTTP_NONWORKING_DELETE(103,"DELETE Method Does Not Work", "deleteDoesNotWork"),
    HTTP_REPEATED_CREATE_PUT(104, "Repeated PUT Creates Resource With 201", "repeatedCreatePut"),

    //2xx: API
    SCHEMA_INVALID_RESPONSE(200, "Received A Response From API That Is Not Valid According To Its Schema", "returnsSchemaInvalidResponse"),

    //3xx: GraphQL
    GQL_ERROR_FIELD(301, "Error Field", "returnedErrors"),

    //4xx: RPC
    // RPC internal error, eg thrift application internal error exception
    RPC_INTERNAL_ERROR(400, "Internal Error", "causesInternalError"),
    // RPC service error which is customized by user
    RPC_SERVICE_ERROR(401, "Service Error", "causesServiceError"),
    // exception for RPC
    RPC_DECLARED_EXCEPTION(402, "Declared Exception", "throwsExpectedException"),
    // unexpected exception for RPC
    RPC_UNEXPECTED_EXCEPTION(403,"Unexpected Exception", "throwsUnexpectedException"),
    // an RPC call which fails to achieve a successful business logic
    RPC_HANDLED_ERROR(404,"Business Logic Error", "failsToExecuteCall"),

    //5xx: Web Frontend
    WEB_BROKEN_LINK(500, "Broken Link", "returnsBrokenLink"),
    //6xx: mobile

    //8xx: security
    SECURITY_EXISTENCE_LEAKAGE(800, "Leakage Information Existence of Protected Resource", "allowsUnauthorizedAccessToProtectedResource"),
    SECURITY_NOT_RECOGNIZED_AUTHENTICATED(801, "Wrongly Not Recognized as Authenticated", "failedToAuthenticateWithValidCredentials"),
    SECURITY_FORBIDDEN_DELETE(802, "Forbidden Delete But Allowed Modifications", "forbidsDeleteButAllowsModifications"),
    SECURITY_FORBIDDEN_PUT(803, "Forbidden Replacement But Allowed Modifications", "forbidsReplacementButAllowsModifications"),
    SECURITY_FORBIDDEN_PATCH(804, "Forbidden Updates But Allowed Modifications", "forbidsUpdatesButAllowsModifications"),
    SECURITY_ALLOW_MODIFICATION_BY_ALL(805, "Resource Created By An User Can Be Modified By All Other Users", "createdResourceCanBeModifiedByEveryone")

    //9xx: undefined
    ;


    /**
     * A unique code identifying this fault category
     */
    public final int code;

    /**
     * A short description to explain the category
     */
    public final String name;

    /**
     * A short label to be used in test case naming when a single fault is found
     */
    public final String testCaseLabel;

    FaultCategory(int code, String name, String testCaseLabel) {
        this.code = code;
        this.name = Objects.requireNonNull(name);
        this.testCaseLabel = Objects.requireNonNull(testCaseLabel);
    }

    /**
     * A descriptive identified for this category.
     * Not a full, lengthy description.
     * For example based on code and name
     */
    public String getLabel() {
        return "F" + code + ":" + name;
    }

}
