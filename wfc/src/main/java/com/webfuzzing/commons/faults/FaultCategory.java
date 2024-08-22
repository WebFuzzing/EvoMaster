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
    ERROR_FIELD(301, "Error Field"),

    //4xx: RPC
    //5xx: Web Frontend
    //6xx: mobile

    //7xx: robustness
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
