package com.webfuzzing.commons.faults;

import java.util.Objects;

public enum DefinedFaultCategory implements FaultCategory {

    /*
        TODO
        code label are still up to discussion and re-arrangement...
     */

    //1xx: HTTP

    HTTP_STATUS_500(100, "HTTP Status 500", "causes500_internalServerError",
            "TODO"),
    SCHEMA_INVALID_RESPONSE(101, "Received A Response From API That Is Not Valid According To Its Schema", "returnsSchemaInvalidResponse",
            "TODO"),


    ;

    private final int code;

    private final String name;

    private final String testCaseLabel;

    private final String fullDescription;

    DefinedFaultCategory(int code, String name, String testCaseLabel, String fullDescription) {
        this.code = code;
        this.name = Objects.requireNonNull(name);
        this.testCaseLabel = Objects.requireNonNull(testCaseLabel);
        this.fullDescription = Objects.requireNonNull(fullDescription);
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

    @Override
    public String getFullDescription() {
        return fullDescription;
    }
}
