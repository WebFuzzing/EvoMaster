package org.evomaster.client.java.controller.opensearch.operations;

/**
 * Represents Wildcard operation.
 * Searches for terms that match a wildcard pattern. Wildcard queries support the following operators:
 * * (asterisk) - Matches zero or more characters
 * ? (question mark) - Matches any single character
 * <p>
 * <a href="https://docs.opensearch.org/latest/query-dsl/term/wildcard/">OpenSearch Wildcard Operation</a>
 */
public class WildcardOperation extends FieldValueOperation {

    public WildcardOperation(String fieldName, String value) {
        this(fieldName, value, CommonQueryParameters.empty());
    }

    public WildcardOperation(String fieldName, String value, Float boost, Boolean caseInsensitive, String rewrite) {
        this(fieldName, value, new CommonQueryParameters(boost, null, rewrite, caseInsensitive));
    }

    public WildcardOperation(String fieldName, String value, CommonQueryParameters commonParams) {
        super(fieldName, value, commonParams);
    }
}
