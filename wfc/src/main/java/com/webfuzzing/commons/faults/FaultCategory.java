package com.webfuzzing.commons.faults;

public enum FaultCategory {

    //1xx: HTTP
    HTTP_STATUS_500(100, "HTTP Status 500"),
    HTTP_INVALID_PAYLOAD_SYNTAX(101, "Invalid Payload Syntax"),
    HTTP_INVALID_LOCATION(102, "Invalid Location HTTP Header"),

    //2xx: REST/OpenAPI
    OPENAPI_UNDECLARED_STATUS(200, "Undeclared HTTP Status Code"),
    INVALID_RESPONSE(201, "Invalid HTTP Response Object"),

    //3xx: GraphQL
    GQL_ERROR_FIELD(301, "Error Field"),

    //4xx: RPC
    // RPC internal error, eg thrift application internal error exception
    RPC_INTERNAL_ERROR(400, "Internal Error"),
    // RPC service error which is customized by user
    RPC_SERVICE_ERROR(401, "Service Error"),
    // exception for RPC
    RPC_DECLARED_EXCEPTION(402, "Declared Exception"),
    // unexpected exception for RPC
    RPC_UNEXPECTED_EXCEPTION(403,"Unexpected Exception"),
    // an RPC call which fails to achieve a successful business logic
    RPC_HANDLED_ERROR(404,"Business Logic Error"),

    //5xx: Web Frontend
    WEB_BROKEN_LINK(500, "Broken Link"),
    //6xx: mobile

    //8xx: security
    //9xx: undefined
    ;

    public final int code;

    public final String name;

    FaultCategory(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getLabel() {
        return "F" + code + ":" + name;
    }
}
