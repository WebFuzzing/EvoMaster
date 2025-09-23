package org.evomaster.client.java.controller.opensearch.operations;

/**
 * Represents Prefix operation.
 * Searches for terms that begin with a specific prefix.
 * <p>
 * <a href="https://docs.opensearch.org/latest/query-dsl/term/prefix/">OpenSearch Prefix Operation</a>
 */
public class PrefixOperation extends FieldValueOperation {

    public PrefixOperation(String fieldName, String value) {
        this(fieldName, value, CommonQueryParameters.empty());
    }

    public PrefixOperation(String fieldName, String value, Float boost, Boolean caseInsensitive, String rewrite) {
        this(fieldName, value, new CommonQueryParameters(boost, null, rewrite, caseInsensitive));
    }

    public PrefixOperation(String fieldName, String value, CommonQueryParameters commonParams) {
        super(fieldName, value, commonParams);
    }
}
