package org.evomaster.client.java.controller.opensearch.operations;

/**
 * Abstract base class for OpenSearch operations that operate on a specific field with a value.
 * This includes operations like Term, Prefix, Wildcard, Fuzzy, and Regexp.
 */
public abstract class FieldValueOperation extends QueryOperation {
    private final String fieldName;
    private final String value;
    private final CommonQueryParameters commonParams;

    protected FieldValueOperation(String fieldName, String value, CommonQueryParameters commonParams) {
        this.fieldName = fieldName;
        this.value = value;
        this.commonParams = commonParams != null ? commonParams : CommonQueryParameters.empty();
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getValue() {
        return value;
    }

    public Float getBoost() {
        return commonParams.getBoost();
    }

    public String getName() {
        return commonParams.getName();
    }

    public String getRewrite() {
        return commonParams.getRewrite();
    }

    public Boolean getCaseInsensitive() {
        return commonParams.getCaseInsensitive();
    }

    protected CommonQueryParameters getCommonParams() {
        return commonParams;
    }
}
