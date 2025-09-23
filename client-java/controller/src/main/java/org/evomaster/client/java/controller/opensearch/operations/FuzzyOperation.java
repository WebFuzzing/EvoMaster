package org.evomaster.client.java.controller.opensearch.operations;

/**
 * Represents Fuzzy operation.
 * Searches for documents containing terms that are similar to the search term within the maximum allowed
 * Damerau-Levenshtein distance. The Damerau-Levenshtein distance measures the number of one-character
 * changes needed to change one term to another term.
 * <p>
 * <a href="https://docs.opensearch.org/latest/query-dsl/term/fuzzy/">OpenSearch Fuzzy Operation</a>
 */
public class FuzzyOperation extends QueryOperation {
    private final String fieldName;
    private final String value;
    private final Float boost;
    private final Integer fuzziness;
    private final Integer maxExpansions;
    private final Integer prefixLength;
    private final Boolean transpositions;
    private final String rewrite;

    public FuzzyOperation(String fieldName, String value) {
        this(fieldName, value, null, null, null, null, null, null);
    }

    public FuzzyOperation(String fieldName, String value, Float boost, Integer fuzziness, 
                         Integer maxExpansions, Integer prefixLength, Boolean transpositions, String rewrite) {
        this.fieldName = fieldName;
        this.value = value;
        this.boost = boost;
        this.fuzziness = fuzziness;
        this.maxExpansions = maxExpansions;
        this.prefixLength = prefixLength;
        this.transpositions = transpositions;
        this.rewrite = rewrite;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getValue() {
        return value;
    }

    public Float getBoost() {
        return boost;
    }

    public Integer getFuzziness() {
        return fuzziness;
    }

    public Integer getMaxExpansions() {
        return maxExpansions;
    }

    public Integer getPrefixLength() {
        return prefixLength;
    }

    public Boolean getTranspositions() {
        return transpositions;
    }

    public String getRewrite() {
        return rewrite;
    }
}
