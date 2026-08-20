package com.webfuzzing.arazzo.models.domain;

/**
 * Representing the model Criterion Expression Type Object
 * An object used to describe the type and version of an expression used within a Criterion Object
 */
public class CriterionExpression {
    private String type;
    private String version;

    public CriterionExpression() {
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
