package org.evomaster.client.java.controller.opensearch.utils;

import org.evomaster.client.java.controller.opensearch.operations.CommonQueryParameters;

import static org.evomaster.client.java.controller.opensearch.utils.OpenSearchQueryHelper.*;

/**
 * Utility class to extract common parameters and reduce code duplication in selectors.
 */
public class ParameterExtractor {

    /**
     * Extract common query parameters (boost, name, rewrite, case_insensitive) from a query object.
     */
    public static CommonQueryParameters extractCommonParameters(Object query, String structure) {
        return new CommonQueryParameters.Builder()
            .boost(extractBoost(query, structure))
            .name(extractQueryName(query, structure))
            .rewrite(extractRewrite(query, structure))
            .caseInsensitive(extractCaseInsensitive(query, structure))
            .build();
    }

    /**
     * Extract field name and value for field-value operations.
     */
    public static FieldValueParams extractFieldValueParams(Object query, String structure) {
        String fieldName = extractFieldName(query, structure);
        String value = (String) extractFieldValue(query, structure);
        CommonQueryParameters commonParams = extractCommonParameters(query, structure);
        
        return new FieldValueParams(fieldName, value, commonParams);
    }

    /**
     * Extract fuzzy-specific parameters.
     */
    public static FuzzyParams extractFuzzyParams(Object query, String structure) {
        FieldValueParams baseParams = extractFieldValueParams(query, structure);
        
        Integer fuzziness = extractIntegerParameter(query, structure, "fuzziness");
        Integer maxExpansions = extractIntegerParameter(query, structure, "maxExpansions");
        Integer prefixLength = extractIntegerParameter(query, structure, "prefixLength");
        Boolean transpositions = extractBooleanParameter(query, structure, "transpositions");
        
        return new FuzzyParams(baseParams, fuzziness, maxExpansions, prefixLength, transpositions);
    }

    /**
     * Extract regexp-specific parameters.
     */
    public static RegexpParams extractRegexpParams(Object query, String structure) {
        FieldValueParams baseParams = extractFieldValueParams(query, structure);
        
        String flags = extractStringParameter(query, structure, "flags");
        Integer maxDeterminizedStates = extractIntegerParameter(query, structure, "maxDeterminizedStates");
        
        return new RegexpParams(baseParams, flags, maxDeterminizedStates);
    }

    /**
     * Extract match-specific parameters.
     */
    public static MatchParams extractMatchParams(Object query, String structure) {
        FieldValueParams baseParams = extractFieldValueParams(query, structure);
        
        String operator = extractStringParameter(query, structure, "operator");
        Integer minimumShouldMatch = extractIntegerParameter(query, structure, "minimumShouldMatch");
        String fuzziness = extractStringParameter(query, structure, "fuzziness");
        Integer prefixLength = extractIntegerParameter(query, structure, "prefixLength");
        Integer maxExpansions = extractIntegerParameter(query, structure, "maxExpansions");
        String analyzer = extractStringParameter(query, structure, "analyzer");
        Boolean fuzzyTranspositions = extractBooleanParameter(query, structure, "fuzzyTranspositions");
        Boolean lenient = extractBooleanParameter(query, structure, "lenient");
        Boolean zeroTermsQuery = extractBooleanParameter(query, structure, "zeroTermsQuery");
        
        return new MatchParams(baseParams, operator, minimumShouldMatch, fuzziness, prefixLength,
                              maxExpansions, analyzer, fuzzyTranspositions, lenient, zeroTermsQuery);
    }

    /**
     * Extract range-specific parameters.
     */
    public static RangeParams extractRangeParams(Object query, String structure) {
        String fieldName = extractFieldName(query, structure);
        Object gte = extractRangeParameter(query, structure, "gte");
        Object gt = extractRangeParameter(query, structure, "gt");
        Object lte = extractRangeParameter(query, structure, "lte");
        Object lt = extractRangeParameter(query, structure, "lt");
        String format = extractRangeStringParameter(query, structure, "format");
        String relation = extractRangeStringParameter(query, structure, "relation");
        Float boost = extractBoost(query, structure);
        String timeZone = extractRangeStringParameter(query, structure, "timeZone");
        
        return new RangeParams(fieldName, gte, gt, lte, lt, format, relation, boost, timeZone);
    }

    // Parameter holder classes
    public static class FieldValueParams {
        public final String fieldName;
        public final String value;
        public final CommonQueryParameters commonParams;

        public FieldValueParams(String fieldName, String value, CommonQueryParameters commonParams) {
            this.fieldName = fieldName;
            this.value = value;
            this.commonParams = commonParams;
        }
    }

    public static class FuzzyParams {
        public final FieldValueParams baseParams;
        public final Integer fuzziness;
        public final Integer maxExpansions;
        public final Integer prefixLength;
        public final Boolean transpositions;

        public FuzzyParams(FieldValueParams baseParams, Integer fuzziness, Integer maxExpansions, 
                          Integer prefixLength, Boolean transpositions) {
            this.baseParams = baseParams;
            this.fuzziness = fuzziness;
            this.maxExpansions = maxExpansions;
            this.prefixLength = prefixLength;
            this.transpositions = transpositions;
        }
    }

    public static class RegexpParams {
        public final FieldValueParams baseParams;
        public final String flags;
        public final Integer maxDeterminizedStates;

        public RegexpParams(FieldValueParams baseParams, String flags, Integer maxDeterminizedStates) {
            this.baseParams = baseParams;
            this.flags = flags;
            this.maxDeterminizedStates = maxDeterminizedStates;
        }
    }

    public static class MatchParams {
        public final FieldValueParams baseParams;
        public final String operator;
        public final Integer minimumShouldMatch;
        public final String fuzziness;
        public final Integer prefixLength;
        public final Integer maxExpansions;
        public final String analyzer;
        public final Boolean fuzzyTranspositions;
        public final Boolean lenient;
        public final Boolean zeroTermsQuery;

        public MatchParams(FieldValueParams baseParams, String operator, Integer minimumShouldMatch,
                          String fuzziness, Integer prefixLength, Integer maxExpansions, String analyzer,
                          Boolean fuzzyTranspositions, Boolean lenient, Boolean zeroTermsQuery) {
            this.baseParams = baseParams;
            this.operator = operator;
            this.minimumShouldMatch = minimumShouldMatch;
            this.fuzziness = fuzziness;
            this.prefixLength = prefixLength;
            this.maxExpansions = maxExpansions;
            this.analyzer = analyzer;
            this.fuzzyTranspositions = fuzzyTranspositions;
            this.lenient = lenient;
            this.zeroTermsQuery = zeroTermsQuery;
        }
    }

    public static class RangeParams {
        public final String fieldName;
        public final Object gte, gt, lte, lt;
        public final String format, relation, timeZone;
        public final Float boost;

        public RangeParams(String fieldName, Object gte, Object gt, Object lte, Object lt,
                          String format, String relation, Float boost, String timeZone) {
            this.fieldName = fieldName;
            this.gte = gte;
            this.gt = gt;
            this.lte = lte;
            this.lt = lt;
            this.format = format;
            this.relation = relation;
            this.boost = boost;
            this.timeZone = timeZone;
        }
    }
}
