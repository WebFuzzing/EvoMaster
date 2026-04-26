package org.evomaster.client.java.controller.opensearch.operations;

/**
 * Represents Match operation.
 * The match query is the standard query for performing full-text searches, including fuzzy matching and phrase or proximity searches.
 * <p>
 * <a href="https://docs.opensearch.org/latest/query-dsl/full-text/match/">OpenSearch Match Operation</a>
 */
public class MatchOperation extends FieldValueOperation {
    private final String operator; // "and" or "or"
    private final Integer minimumShouldMatch;
    private final String fuzziness;
    private final Integer prefixLength;
    private final Integer maxExpansions;
    private final String analyzer;
    private final Boolean fuzzyTranspositions;
    private final Boolean lenient;
    private final Boolean zeroTermsQuery;
    private final Float cutoffFrequency;

    public MatchOperation(String fieldName, String value) {
        this(fieldName, value, CommonQueryParameters.empty(), null, null, null, null, null, null, null, null, null, null);
    }

    public MatchOperation(String fieldName, String value, CommonQueryParameters commonParams,
                         String operator, Integer minimumShouldMatch, String fuzziness, Integer prefixLength,
                         Integer maxExpansions, String analyzer, Boolean fuzzyTranspositions, 
                         Boolean lenient, Boolean zeroTermsQuery, Float cutoffFrequency) {
        super(fieldName, value, commonParams);
        this.operator = operator;
        this.minimumShouldMatch = minimumShouldMatch;
        this.fuzziness = fuzziness;
        this.prefixLength = prefixLength;
        this.maxExpansions = maxExpansions;
        this.analyzer = analyzer;
        this.fuzzyTranspositions = fuzzyTranspositions;
        this.lenient = lenient;
        this.zeroTermsQuery = zeroTermsQuery;
        this.cutoffFrequency = cutoffFrequency;
    }

    public String getOperator() {
        return operator;
    }

    public Integer getMinimumShouldMatch() {
        return minimumShouldMatch;
    }

    public String getFuzziness() {
        return fuzziness;
    }

    public Integer getPrefixLength() {
        return prefixLength;
    }

    public Integer getMaxExpansions() {
        return maxExpansions;
    }

    public String getAnalyzer() {
        return analyzer;
    }

    public Boolean getFuzzyTranspositions() {
        return fuzzyTranspositions;
    }

    public Boolean getLenient() {
        return lenient;
    }

    public Boolean getZeroTermsQuery() {
        return zeroTermsQuery;
    }

    public Float getCutoffFrequency() {
        return cutoffFrequency;
    }
}
