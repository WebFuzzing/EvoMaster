package org.evomaster.client.java.controller.opensearch.operations;

/**
 * Represents Regexp operation.
 * Searches for terms that match a regular expression. Regular expressions are applied to the terms
 * (that is, tokens) in the field—not to the entire field.
 * <p>
 * <a href="https://docs.opensearch.org/latest/query-dsl/term/regexp/">OpenSearch Regexp Operation</a>
 */
public class RegexpOperation extends FieldValueOperation {
    private final String flags;
    private final Integer maxDeterminizedStates;

    public RegexpOperation(String fieldName, String value) {
        this(fieldName, value, null, null, null, null, null);
    }

    public RegexpOperation(String fieldName, String value, Float boost, Boolean caseInsensitive, 
                          String flags, Integer maxDeterminizedStates, String rewrite) {
        super(fieldName, value, new CommonQueryParameters(boost, null, rewrite, caseInsensitive));
        this.flags = flags;
        this.maxDeterminizedStates = maxDeterminizedStates;
    }

    public RegexpOperation(String fieldName, String value, CommonQueryParameters commonParams,
                          String flags, Integer maxDeterminizedStates) {
        super(fieldName, value, commonParams);
        this.flags = flags;
        this.maxDeterminizedStates = maxDeterminizedStates;
    }

    public String getFlags() {
        return flags;
    }

    public Integer getMaxDeterminizedStates() {
        return maxDeterminizedStates;
    }
}
