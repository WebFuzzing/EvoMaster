package org.evomaster.client.java.controller.opensearch;

import java.util.List;
import java.util.Map;
import java.util.function.DoubleUnaryOperator;
import org.evomaster.client.java.controller.opensearch.operations.ComparisonOperation;
import org.evomaster.client.java.controller.opensearch.operations.TermOperation;
import org.evomaster.client.java.controller.opensearch.operations.TermsOperation;
import org.evomaster.client.java.controller.opensearch.operations.TermsSetOperation;
import org.evomaster.client.java.controller.opensearch.operations.IdsOperation;
import org.evomaster.client.java.controller.opensearch.operations.RangeOperation;
import org.evomaster.client.java.controller.opensearch.operations.PrefixOperation;
import org.evomaster.client.java.controller.opensearch.operations.ExistsOperation;
import org.evomaster.client.java.controller.opensearch.operations.FuzzyOperation;
import org.evomaster.client.java.controller.opensearch.operations.WildcardOperation;
import org.evomaster.client.java.controller.opensearch.operations.RegexpOperation;
import org.evomaster.client.java.controller.opensearch.operations.BoolOperation;
import org.evomaster.client.java.controller.opensearch.operations.MatchOperation;
import org.evomaster.client.java.controller.opensearch.operations.QueryOperation;
import org.evomaster.client.java.distance.heuristics.DistanceHelper;
import org.evomaster.client.java.sql.internal.TaintHandler;
import org.evomaster.client.java.utils.SimpleLogger;

public class OpenSearchHeuristicsCalculator {
    private final TaintHandler taintHandler;

    public OpenSearchHeuristicsCalculator() {
        this(null);
    }

    public OpenSearchHeuristicsCalculator(TaintHandler taintHandler) {
        this.taintHandler = taintHandler;
    }

    public double computeExpression(Object query, Object doc) {
        QueryOperation operation = getOperation(query);
        return calculateDistance(operation, doc);
    }

    private QueryOperation getOperation(Object query) {
        return new OpenSearchQueryParser().parse(query);
    }

    protected double calculateDistance(QueryOperation operation, Object doc) {
        if (operation instanceof TermOperation<?>) {
            return calculateDistanceForEquals((TermOperation<?>) operation, doc);
        } else if (operation instanceof TermsOperation<?>) {
            return calculateDistanceForTerms((TermsOperation<?>) operation, doc);
        } else if (operation instanceof TermsSetOperation<?>) {
            return calculateDistanceForTermsSet((TermsSetOperation<?>) operation, doc);
        } else if (operation instanceof IdsOperation) {
            return calculateDistanceForIds((IdsOperation) operation, doc);
        } else if (operation instanceof RangeOperation) {
            return calculateDistanceForRange((RangeOperation) operation, doc);
        } else if (operation instanceof PrefixOperation) {
            return calculateDistanceForPrefix((PrefixOperation) operation, doc);
        } else if (operation instanceof ExistsOperation) {
            return calculateDistanceForExists((ExistsOperation) operation, doc);
        } else if (operation instanceof FuzzyOperation) {
            return calculateDistanceForFuzzy((FuzzyOperation) operation, doc);
        } else if (operation instanceof WildcardOperation) {
            return calculateDistanceForWildcard((WildcardOperation) operation, doc);
        } else if (operation instanceof RegexpOperation) {
            return calculateDistanceForRegexp((RegexpOperation) operation, doc);
        } else if (operation instanceof BoolOperation) {
            return calculateDistanceForBool((BoolOperation) operation, doc);
        } else if (operation instanceof MatchOperation) {
            return calculateDistanceForMatch((MatchOperation) operation, doc);
        } else {
            SimpleLogger.warn("Unsupported operation type: " + operation.getClass().getName());
            return Double.MAX_VALUE;
        }
    }

    private double calculateDistanceForEquals(TermOperation<?> operation, Object doc) {
        return calculateDistanceForComparisonOperation(operation, doc, (Math::abs));
    }

    /**
     * Calculate distance for Terms operation.
     * A Terms query matches if the document field value matches ANY of the terms in the list.
     * Distance is the minimum distance to any of the terms.
     */
    private double calculateDistanceForTerms(TermsOperation<?> operation, Object doc) {
        String field = operation.getFieldName();
        List<?> expectedValues = operation.getValues();

        if (!((Map<?,?>) doc).containsKey(field)) {
            return Double.MAX_VALUE;
        }

        Object actualValue = ((Map<?,?>) doc).get(field);
        
        // Find the minimum distance to any of the terms
        double minDistance = Double.MAX_VALUE;
        for (Object expectedValue : expectedValues) {
            double distance = compareValues(actualValue, expectedValue, false);
            if (distance == 0) {
                return 0.0;
            }
            if (distance < minDistance) {
                minDistance = distance;
            }
        }
        
        return Math.abs(minDistance);
    }

    /**
     * Calculate distance for TermsSet operation.
     * A TermsSet query matches if the document field contains at least the minimum required number of matching terms.
     * For heuristic calculation, we simulate the minimum requirement by counting actual matches and calculating distance
     * based on how many more matches are needed.
     */
    private double calculateDistanceForTermsSet(TermsSetOperation<?> operation, Object doc) {
        String field = operation.getFieldName();
        List<?> expectedTerms = operation.getTerms();
        String minimumShouldMatchField = operation.getMinimumShouldMatchField();

        if (!((Map<?,?>) doc).containsKey(field)) {
            return Double.MAX_VALUE;
        }

        // Handle empty terms list
        if (expectedTerms.isEmpty()) {
            return Double.MAX_VALUE;
        }

        Object actualValue = ((Map<?,?>) doc).get(field);
        
        // Count how many terms match
        int matchCount = 0;
        double totalDistance = 0.0;
        
        for (Object expectedTerm : expectedTerms) {
            double distance = compareValues(actualValue, expectedTerm, false);
            if (distance == 0) {
                matchCount++;
            } else {
                totalDistance += distance;
            }
        }
        
        // Get minimum required matches - for simplicity, we assume it's available in the document
        // In a real scenario, this would be more complex involving field lookup or script evaluation
        int minimumRequired = getMinimumShouldMatch(doc, minimumShouldMatchField);
        
        if (matchCount >= minimumRequired) {
            // Requirement satisfied
            return 0.0;
        } else {
            // Not enough matches - distance based on how many more matches needed
            int shortfall = minimumRequired - matchCount;
            // Return a distance that reflects both the shortfall and the quality of non-matching terms
            return shortfall * 10.0 + (totalDistance / expectedTerms.size());
        }
    }

    /**
     * Helper method to get the minimum should match value from the document or use a default.
     * In real scenarios, this would involve more complex logic for field lookup or script evaluation.
     */
    private int getMinimumShouldMatch(Object doc, String minimumShouldMatchField) {
        if (minimumShouldMatchField != null && ((Map<?,?>) doc).containsKey(minimumShouldMatchField)) {
            Object value = ((Map<?,?>) doc).get(minimumShouldMatchField);
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
        }
        // Default to 1 if no field specified or found
        return 1;
    }

    private double calculateDistanceForComparisonOperation(ComparisonOperation<?> operation, Object doc, DoubleUnaryOperator calculateDistance) {
        Object expectedValue = operation.getValue();
        String field = operation.getFieldName();

        if (!((Map<?,?>) doc).containsKey(field)) {
            return Double.MAX_VALUE;
        }

        Object actualValue = ((Map<?,?>) doc).get(field);
        
        // Handle case sensitivity for TermOperation
        boolean caseInsensitive = false;
        if (operation instanceof TermOperation<?>) {
            caseInsensitive = ((TermOperation<?>) operation).getCaseInsensitive();
        }
        
        double dif = compareValues(actualValue, expectedValue, caseInsensitive);

        return calculateDistance.applyAsDouble(dif);
    }

    private double compareValues(Object val1, Object val2) {
        return compareValues(val1, val2, false);
    }

    private double compareValues(Object val1, Object val2, boolean caseInsensitive) {

        if (val1 instanceof Number && val2 instanceof Number) {
            double x = ((Number) val1).doubleValue();
            double y = ((Number) val2).doubleValue();
            return x - y;
        }

        if (val1 instanceof String && val2 instanceof String) {
            String str1 = (String) val1;
            String str2 = (String) val2;
            
            // Apply case insensitive comparison if needed
            if (caseInsensitive) {
                str1 = str1.toLowerCase();
                str2 = str2.toLowerCase();
            }

            if (taintHandler != null) {
                taintHandler.handleTaintForStringEquals(str1, str2, false);
            }

            return (double) DistanceHelper.getLeftAlignmentDistance(str1, str2);
        }

        if (val1 instanceof Boolean && val2 instanceof Boolean) {
            return val1 == val2 ? 0d : 1d;
        }

        if (val1 instanceof String && isObjectId(val2)) {
            String str1 = (String) val1;
            String str2 = val2.toString();
            
            // Apply case insensitive comparison if needed
            if (caseInsensitive) {
                str1 = str1.toLowerCase();
                str2 = str2.toLowerCase();
            }
            
            if(taintHandler!=null){
                taintHandler.handleTaintForStringEquals(str1, str2, false);
            }
            return (double) DistanceHelper.getLeftAlignmentDistance(str1, str2);
        }

        if (val2 instanceof String && isObjectId(val1)) {
            String str1 = val1.toString();
            String str2 = (String) val2;
            
            // Apply case insensitive comparison if needed
            if (caseInsensitive) {
                str1 = str1.toLowerCase();
                str2 = str2.toLowerCase();
            }
            
            if (taintHandler != null) {
                taintHandler.handleTaintForStringEquals(str1, str2, false);
            }
            return (double) DistanceHelper.getLeftAlignmentDistance(str1, str2);
        }

        if (isObjectId(val2) && isObjectId(val1)) {
            String str1 = val1.toString();
            String str2 = val2.toString();
            
            // Apply case insensitive comparison if needed
            if (caseInsensitive) {
                str1 = str1.toLowerCase();
                str2 = str2.toLowerCase();
            }
            
            return (double) DistanceHelper.getLeftAlignmentDistance(str1, str2);
        }


        if (val1 instanceof List<?> && val2 instanceof List<?>) {
            // Modify
            return Double.MAX_VALUE;
        }

        return Double.MAX_VALUE;
    }

    /**
     * Calculate distance for IDs operation.
     * IDs query matches if the document _id field matches any of the specified IDs.
     */
    private double calculateDistanceForIds(IdsOperation operation, Object doc) {
        List<String> expectedIds = operation.getValues();
        
        // Get document ID - assuming it's available in the document as "_id"
        if (!((Map<?,?>) doc).containsKey("_id")) {
            return Double.MAX_VALUE;
        }
        
        Object actualId = ((Map<?,?>) doc).get("_id");
        String actualIdStr = actualId.toString();
        
        // Find minimum distance to any of the expected IDs
        double minDistance = Double.MAX_VALUE;
        for (String expectedId : expectedIds) {
            double distance = compareValues(actualIdStr, expectedId, false);
            if (distance == 0) {
                return 0.0;
            }
            if (distance < minDistance) {
                minDistance = distance;
            }
        }
        
        return Math.abs(minDistance);
    }

    /**
     * Calculate distance for Range operation.
     * Range query matches if the document field value is within the specified range.
     */
    private double calculateDistanceForRange(RangeOperation operation, Object doc) {
        String field = operation.getFieldName();
        
        if (!((Map<?,?>) doc).containsKey(field)) {
            return Double.MAX_VALUE;
        }
        
        Object actualValue = ((Map<?,?>) doc).get(field);
        
        // Convert to numeric value for comparison
        if (!(actualValue instanceof Number)) {
            // For non-numeric values, try to parse as double
            try {
                actualValue = Double.parseDouble(actualValue.toString());
            } catch (NumberFormatException e) {
                return Double.MAX_VALUE;
            }
        }
        
        double actualNum = ((Number) actualValue).doubleValue();
        double distance = 0.0;
        
        // Check greater than or equal to (gte)
        if (operation.getGte() != null) {
            double gteValue = ((Number) operation.getGte()).doubleValue();
            if (actualNum < gteValue) {
                distance += gteValue - actualNum;
            }
        }
        
        // Check greater than (gt)
        if (operation.getGt() != null) {
            double gtValue = ((Number) operation.getGt()).doubleValue();
            if (actualNum <= gtValue) {
                distance += gtValue - actualNum + 1;
            }
        }
        
        // Check less than or equal to (lte)
        if (operation.getLte() != null) {
            double lteValue = ((Number) operation.getLte()).doubleValue();
            if (actualNum > lteValue) {
                distance += actualNum - lteValue;
            }
        }
        
        // Check less than (lt)
        if (operation.getLt() != null) {
            double ltValue = ((Number) operation.getLt()).doubleValue();
            if (actualNum >= ltValue) {
                distance += actualNum - ltValue + 1;
            }
        }
        
        return distance;
    }

    /**
     * Calculate distance for Prefix operation.
     * Prefix query matches if the document field value starts with the specified prefix.
     */
    private double calculateDistanceForPrefix(PrefixOperation operation, Object doc) {
        String field = operation.getFieldName();
        String expectedPrefix = operation.getValue();
        Boolean caseInsensitive = operation.getCaseInsensitive();
        
        if (!((Map<?,?>) doc).containsKey(field)) {
            return Double.MAX_VALUE;
        }
        
        Object actualValue = ((Map<?,?>) doc).get(field);
        String actualStr = actualValue.toString();
        
        // Apply case insensitive comparison if needed
        if (caseInsensitive != null && caseInsensitive) {
            actualStr = actualStr.toLowerCase();
            expectedPrefix = expectedPrefix.toLowerCase();
        }
        
        if (actualStr.startsWith(expectedPrefix)) {
            return 0.0;
        }
        
        // Calculate prefix distance using left alignment
        return (double) DistanceHelper.getLeftAlignmentDistance(actualStr, expectedPrefix);
    }

    /**
     * Calculate distance for Exists operation.
     * Exists query matches if the document contains the specified field.
     */
    private double calculateDistanceForExists(ExistsOperation operation, Object doc) {
        String field = operation.getField();
        
        if (((Map<?,?>) doc).containsKey(field)) {
            Object value = ((Map<?,?>) doc).get(field);
            // Field exists and is not null
            if (value != null) {
                return 0.0;
            }
        }
        
        // Field doesn't exist or is null
        return 1.0;
    }

    /**
     * Calculate distance for Fuzzy operation.
     * Fuzzy query matches if the document field value is within the specified edit distance (fuzziness).
     */
    private double calculateDistanceForFuzzy(FuzzyOperation operation, Object doc) {
        String field = operation.getFieldName();
        String expectedValue = operation.getValue();
        Integer fuzziness = operation.getFuzziness();
        Boolean transpositions = operation.getTranspositions();
        
        if (!((Map<?,?>) doc).containsKey(field)) {
            return Double.MAX_VALUE;
        }
        
        Object actualValue = ((Map<?,?>) doc).get(field);
        String actualStr = actualValue.toString();
        
        // Calculate edit distance (Levenshtein distance)
        int editDistance = calculateEditDistance(actualStr, expectedValue, transpositions != null ? transpositions : true);
        
        // If fuzziness is specified, check if within allowed distance
        if (fuzziness != null) {
            if (editDistance <= fuzziness) {
                return 0.0;
            } else {
                return editDistance - fuzziness;
            }
        }
        
        // Default fuzziness is AUTO, which is typically 0, 1, or 2 based on term length
        int defaultFuzziness = getAutoFuzziness(expectedValue.length());
        if (editDistance <= defaultFuzziness) {
            return 0.0;
        }
        
        return editDistance - defaultFuzziness;
    }

    /**
     * Calculate distance for Wildcard operation.
     * Wildcard query matches if the document field value matches the wildcard pattern.
     */
    private double calculateDistanceForWildcard(WildcardOperation operation, Object doc) {
        String field = operation.getFieldName();
        String pattern = operation.getValue();
        Boolean caseInsensitive = operation.getCaseInsensitive();
        
        if (!((Map<?,?>) doc).containsKey(field)) {
            return Double.MAX_VALUE;
        }
        
        Object actualValue = ((Map<?,?>) doc).get(field);
        String actualStr = actualValue.toString();
        
        // Apply case insensitive comparison if needed
        if (caseInsensitive != null && caseInsensitive) {
            actualStr = actualStr.toLowerCase();
            pattern = pattern.toLowerCase();
        }
        
        if (matchesWildcard(actualStr, pattern)) {
            return 0.0;
        }
        
        // Calculate approximate distance based on pattern similarity
        return (double) DistanceHelper.getLeftAlignmentDistance(actualStr, pattern.replace("*", "").replace("?", ""));
    }

    /**
     * Calculate distance for Regexp operation.
     * Regexp query matches if the document field value matches the regular expression.
     */
    private double calculateDistanceForRegexp(RegexpOperation operation, Object doc) {
        String field = operation.getFieldName();
        String regex = operation.getValue();
        Boolean caseInsensitive = operation.getCaseInsensitive();
        
        if (!((Map<?,?>) doc).containsKey(field)) {
            return Double.MAX_VALUE;
        }
        
        Object actualValue = ((Map<?,?>) doc).get(field);
        String actualStr = actualValue.toString();
        
        try {
            // Create pattern with case insensitive flag if needed
            java.util.regex.Pattern pattern;
            if (caseInsensitive != null && caseInsensitive) {
                pattern = java.util.regex.Pattern.compile(regex, java.util.regex.Pattern.CASE_INSENSITIVE);
            } else {
                pattern = java.util.regex.Pattern.compile(regex);
            }
            
            if (pattern.matcher(actualStr).matches()) {
                return 0.0;
            }
            
            // For non-matches, return a distance based on string similarity
            return (double) DistanceHelper.getLeftAlignmentDistance(actualStr, regex);
            
        } catch (java.util.regex.PatternSyntaxException e) {
            // Invalid regex pattern
            return Double.MAX_VALUE;
        }
    }

    /**
     * Calculate distance for Bool operation.
     * Bool query combines multiple clauses with Boolean logic.
     */
    private double calculateDistanceForBool(BoolOperation operation, Object doc) {
        double totalDistance = 0.0;
        
        // MUST clauses - all must match (AND logic)
        List<QueryOperation> mustClauses = operation.getMust();
        if (mustClauses != null && !mustClauses.isEmpty()) {
            for (QueryOperation clause : mustClauses) {
                double distance = calculateDistance(clause, doc);
                if (distance == Double.MAX_VALUE) {
                    return Double.MAX_VALUE; // If any must clause fails completely
                }
                totalDistance += distance;
            }
        }
        
        // MUST_NOT clauses - none must match (NOT logic)
        List<QueryOperation> mustNotClauses = operation.getMustNot();
        if (mustNotClauses != null && !mustNotClauses.isEmpty()) {
            for (QueryOperation clause : mustNotClauses) {
                double distance = calculateDistance(clause, doc);
                if (distance == 0.0) {
                    return Double.MAX_VALUE; // If any must_not clause matches
                }
                // For must_not, closer matches are worse, so we invert the distance logic
                totalDistance += Math.max(0, 10.0 - distance);
            }
        }
        
        // SHOULD clauses - at least minimum_should_match must match (OR logic)
        List<QueryOperation> shouldClauses = operation.getShould();
        if (shouldClauses != null && !shouldClauses.isEmpty()) {
            int minimumShouldMatch = operation.getMinimumShouldMatch() != null ? 
                operation.getMinimumShouldMatch() : 1;
            
            // Sort should clauses by their distances
            List<Double> shouldDistances = new java.util.ArrayList<>();
            for (QueryOperation clause : shouldClauses) {
                shouldDistances.add(calculateDistance(clause, doc));
            }
            shouldDistances.sort(Double::compareTo);
            
            // Check if we have enough matches
            int matches = 0;
            for (double distance : shouldDistances) {
                if (distance == 0.0) matches++;
            }
            
            if (matches < minimumShouldMatch) {
                // Add penalty for not meeting minimum should match
                totalDistance += (minimumShouldMatch - matches) * 10.0;
                // Add distance from best non-matching clauses
                for (int i = matches; i < Math.min(minimumShouldMatch, shouldDistances.size()); i++) {
                    totalDistance += shouldDistances.get(i);
                }
            }
        }
        
        // FILTER clauses - all must match but don't contribute to score
        List<QueryOperation> filterClauses = operation.getFilter();
        if (filterClauses != null && !filterClauses.isEmpty()) {
            for (QueryOperation clause : filterClauses) {
                double distance = calculateDistance(clause, doc);
                if (distance != 0.0) {
                    return Double.MAX_VALUE; // Filter clauses must match exactly
                }
            }
        }
        
        return totalDistance;
    }

    /**
     * Calculate distance for Match operation.
     * Match query performs full-text search with analysis and scoring.
     */
    private double calculateDistanceForMatch(MatchOperation operation, Object doc) {
        String field = operation.getFieldName();
        String queryText = operation.getValue();
        String operator = operation.getOperator();
        
        if (!((Map<?,?>) doc).containsKey(field)) {
            return Double.MAX_VALUE;
        }
        
        Object actualValue = ((Map<?,?>) doc).get(field);
        String actualStr = actualValue.toString();
        
        // Simple tokenization and matching logic
        String[] queryTokens = queryText.toLowerCase().split("\\s+");
        String[] docTokens = actualStr.toLowerCase().split("\\s+");
        
        if ("and".equals(operator)) {
            // All query tokens must match (AND logic)
            double totalDistance = 0.0;
            for (String queryToken : queryTokens) {
                double minTokenDistance = Double.MAX_VALUE;
                for (String docToken : docTokens) {
                    double distance = DistanceHelper.getLeftAlignmentDistance(docToken, queryToken);
                    if (distance == 0) {
                        minTokenDistance = 0;
                        break;
                    }
                    if (distance < minTokenDistance) {
                        minTokenDistance = distance;
                    }
                }
                totalDistance += minTokenDistance;
            }
            return totalDistance;
        } else {
            // At least one query token must match (OR logic - default)
            double minDistance = Double.MAX_VALUE;
            for (String queryToken : queryTokens) {
                for (String docToken : docTokens) {
                    double distance = DistanceHelper.getLeftAlignmentDistance(docToken, queryToken);
                    if (distance == 0) {
                        return 0.0; // Perfect match found
                    }
                    if (distance < minDistance) {
                        minDistance = distance;
                    }
                }
            }
            return minDistance;
        }
    }

    // Helper methods for advanced operations
    
    private int calculateEditDistance(String s1, String s2, boolean allowTranspositions) {
        if (allowTranspositions) {
            return calculateDamerauLevenshteinDistance(s1, s2);
        } else {
            return calculateLevenshteinDistance(s1, s2);
        }
    }
    
    private int calculateLevenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        
        for (int i = 0; i <= s1.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= s2.length(); j++) dp[0][j] = j;
        
        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                if (s1.charAt(i-1) == s2.charAt(j-1)) {
                    dp[i][j] = dp[i-1][j-1];
                } else {
                    dp[i][j] = 1 + Math.min(Math.min(dp[i-1][j], dp[i][j-1]), dp[i-1][j-1]);
                }
            }
        }
        
        return dp[s1.length()][s2.length()];
    }
    
    private int calculateDamerauLevenshteinDistance(String s1, String s2) {
        // Simplified Damerau-Levenshtein distance (includes transpositions)
        int len1 = s1.length();
        int len2 = s2.length();
        int[][] dp = new int[len1 + 1][len2 + 1];
        
        for (int i = 0; i <= len1; i++) dp[i][0] = i;
        for (int j = 0; j <= len2; j++) dp[0][j] = j;
        
        for (int i = 1; i <= len1; i++) {
            for (int j = 1; j <= len2; j++) {
                int cost = (s1.charAt(i-1) == s2.charAt(j-1)) ? 0 : 1;
                
                dp[i][j] = Math.min(Math.min(
                    dp[i-1][j] + 1,      // deletion
                    dp[i][j-1] + 1),     // insertion
                    dp[i-1][j-1] + cost  // substitution
                );
                
                // Transposition
                if (i > 1 && j > 1 && 
                    s1.charAt(i-1) == s2.charAt(j-2) && 
                    s1.charAt(i-2) == s2.charAt(j-1)) {
                    dp[i][j] = Math.min(dp[i][j], dp[i-2][j-2] + cost);
                }
            }
        }
        
        return dp[len1][len2];
    }
    
    private int getAutoFuzziness(int termLength) {
        if (termLength <= 2) return 0;
        if (termLength <= 5) return 1;
        return 2;
    }
    
    private boolean matchesWildcard(String text, String pattern) {
        // Convert wildcard pattern to regex
        String regex = pattern
            .replace(".", "\\.")
            .replace("*", ".*")
            .replace("?", ".");
        
        try {
            return text.matches(regex);
        } catch (java.util.regex.PatternSyntaxException e) {
            return false;
        }
    }

    private static boolean isObjectId(Object obj) {
        return obj.getClass().getName().equals("org.bson.types.ObjectId");
    }

}
