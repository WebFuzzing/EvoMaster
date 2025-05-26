package com.webfuzzing.commons.faults;

import java.util.Objects;

public enum DefinedFaultCategory implements FaultCategory {

    /*
        TODO
        code label are still up to discussion and re-arrangement...
     */

    //1xx: HTTP

    HTTP_STATUS_500(100, "HTTP Status 500", "causes500_internalServerError"),
    SCHEMA_INVALID_RESPONSE(101, "Received A Response From API That Is Not Valid According To Its Schema", "returnsSchemaInvalidResponse"),


    ;

    private final int code;

    private final String name;

    private final String testCaseLabel;


    DefinedFaultCategory(int code, String name, String testCaseLabel) {
        this.code = code;
        this.name = Objects.requireNonNull(name);
        this.testCaseLabel = Objects.requireNonNull(testCaseLabel);
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getDescriptiveName() {
        return name;
    }

    @Override
    public String getTestCaseLabel() {
        return testCaseLabel;
    }
}
