package org.evomaster.client.java.controller.redis;

import org.evomaster.client.java.controller.internal.db.redis.RedisDistanceWithMetrics;
import org.evomaster.client.java.distance.heuristics.DistanceHelper;
import org.evomaster.client.java.distance.heuristics.Truthness;
import org.evomaster.client.java.distance.heuristics.TruthnessUtils;
import org.evomaster.client.java.instrumentation.RedisCommand;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.RegexDistanceUtils;
import org.evomaster.client.java.sql.internal.TaintHandler;
import org.evomaster.client.java.utils.SimpleLogger;

import java.util.*;

import static org.evomaster.client.java.controller.redis.RedisUtils.redisPatternToRegex;
import static org.evomaster.client.java.distance.heuristics.DistanceHelper.H_MAX_VALUE;
import static org.evomaster.client.java.distance.heuristics.DistanceHelper.H_MIN_VALUE;

public class RedisHeuristicsCalculator {

    private final TaintHandler taintHandler;

    public RedisHeuristicsCalculator() {
        this(null);
    }

    public RedisHeuristicsCalculator(TaintHandler taintHandler) {
        this.taintHandler = taintHandler;
    }

    /**
     * Computes the heuristic distance for a given Redis command.
     * RedisDistance(cmd) = 1 - H_Redis(cmd).ofTrue
     *
     * The RedisKeyValueStore is pre-filtered by type in RedisHandler before reaching here:
     * - GET    → only STRING keys, null values
     * - HGETALL → only HASH keys, null values
     * - SMEMBERS → only SET keys, null values
     * - HGET   → only HASH keys, values contain fields
     * - SINTER → only SET keys, values contain members
     * - KEYS/EXISTS → all keys, null values
     */
    public RedisDistanceWithMetrics computeDistance(RedisCommand redisCommand,
                                                    RedisKeyValueStore redisData) {
        RedisCommand.RedisCommandType type = redisCommand.getType();
        try {
            Truthness t;
            switch (type) {
                case EXISTS:
                case GET:
                case HGETALL:
                case SMEMBERS: {
                    String target = redisCommand.extractArgs().get(0);
                    t = hKeyMatch(target, redisData.getData());
                    return toMetrics(t, redisData.getData().size());
                }

                case HGET: {
                    String key = redisCommand.extractArgs().get(0);
                    String field = redisCommand.extractArgs().get(1);
                    t = TruthnessUtils.buildAndAggregationTruthness(
                            hKeyMatch(key, redisData.getData()),
                            hFieldMatch(field, redisData.getData().get(key))
                    );
                    return toMetrics(t, redisData.getData().size());
                }

                case KEYS: {
                    String pattern = redisCommand.extractArgs().get(0);
                    t = hKeys(pattern, redisData.getData());
                    return toMetrics(t, redisData.getData().size());
                }

                case SINTER: {
                    t = hSinter(redisCommand.extractArgs(), redisData.getData());
                    return toMetrics(t, redisData.getData().size());
                }

                case FT_SEARCH: {
                    String query = redisCommand.extractArgs().get(1);
                    t = hFtSearch(query, redisData.getData());
                    return toMetrics(t, redisData.getData().size());
                }

                case FT_AGGREGATE: {
                    String query = redisCommand.extractArgs().get(1);
                    List<String> groupByFields = redisCommand.extractGroupByFields();
                    t = hFtAggregate(query, groupByFields, redisData.getData());
                    return toMetrics(t, redisData.getData().size());
                }

                default:
                    SimpleLogger.error("Unsupported command type: " + type);
                    throw new IllegalArgumentException("Unsupported command type in Redis heuristic calculation.");
            }
        } catch (Exception e) {
            SimpleLogger.warn("Could not compute distance for " + type + ": " + e.getMessage());
            return new RedisDistanceWithMetrics(H_MAX_VALUE, 0);
        }
    }

    private RedisDistanceWithMetrics toMetrics(Truthness t, int evaluated) {
        return new RedisDistanceWithMetrics(H_MAX_VALUE - t.getOfTrue(), evaluated);
    }

    /**
     * H_key_match(key, db) =
     *   IF db is empty THEN C_FALSE
     *   ELSE IF maxOfTrue == 1 THEN TRUE_C
     *   ELSE scaleTrue(C, maxOfTrue)
     *   where maxOfTrue = max{ getStringEquals(key, k').ofTrue | k' in keys(db) }
     */
    private Truthness hKeyMatch(String targetKey, Map<String, RedisValueData> db) {
        if (db.isEmpty()) {
            return TruthnessUtils.FALSE_TRUTHNESS;
        }

        double maxOfTrue = H_MIN_VALUE;
        for (String key : db.keySet()) {
            Truthness eq = TruthnessUtils.getStringEqualityTruthness(targetKey, key);
            if (taintHandler != null) {
                taintHandler.handleTaintForStringEquals(targetKey, key, false);
            }
            maxOfTrue = Math.max(maxOfTrue, eq.getOfTrue());
            if (maxOfTrue == H_MAX_VALUE) return TruthnessUtils.TRUE_TRUTHNESS;
        }
        return TruthnessUtils.buildScaledTruthness(DistanceHelper.H_NOT_NULL, maxOfTrue);
    }

    /**
     * H_field_match(field, value) =
     *   IF value=nil OR value has no fields THEN C_FALSE
     *   ELSE IF maxOfTrue == 1 THEN TRUE_C
     *   ELSE scaleTrue(C, maxOfTrue)
     *   where maxOfTrue = max{ getStringEquals(field, field').ofTrue | field' in fields(value) }
     */
    private Truthness hFieldMatch(String targetField, RedisValueData value) {
        if (value == null || value.getFields() == null || value.getFields().isEmpty()) {
            return TruthnessUtils.FALSE_TRUTHNESS;
        }

        double maxOfTrue = H_MIN_VALUE;
        for (String field : value.getFields().keySet()) {
            Truthness eq = TruthnessUtils.getStringEqualityTruthness(targetField, field);
            if (taintHandler != null) {
                taintHandler.handleTaintForStringEquals(targetField, field, false);
            }
            maxOfTrue = Math.max(maxOfTrue, eq.getOfTrue());
            if (maxOfTrue == H_MAX_VALUE) return TruthnessUtils.TRUE_TRUTHNESS;
        }
        return TruthnessUtils.buildScaledTruthness(DistanceHelper.H_NOT_NULL, maxOfTrue);
    }

    /**
     * H_KEYS(pattern, db) =
     *   IF db is empty THEN C_FALSE
     *   ELSE IF maxPatternSimilarity == 1 THEN TRUE_C
     *   ELSE scaleTrue(C, maxPatternSimilarity)
     *   where patternSimilarity(pattern, key) = 1 - normalizeValue(regexDistance(key, regex))
     */
    private Truthness hKeys(String pattern, Map<String, RedisValueData> db) {
        if (db.isEmpty()) {
            return TruthnessUtils.FALSE_TRUTHNESS;
        }

        String regex;
        try {
            regex = redisPatternToRegex(pattern);
        } catch (IllegalArgumentException e) {
            SimpleLogger.uniqueWarn("Invalid Redis pattern: " + pattern);
            return TruthnessUtils.FALSE_TRUTHNESS;
        }

        double maxPatternSimilarity = H_MIN_VALUE;
        for (String key : db.keySet()) {
            double similarity = H_MAX_VALUE - TruthnessUtils.normalizeValue(
                    RegexDistanceUtils.getStandardDistance(key, regex));
            if (taintHandler != null) {
                taintHandler.handleTaintForRegex(key, regex);
            }
            maxPatternSimilarity = Math.max(maxPatternSimilarity, similarity);
            if (maxPatternSimilarity == H_MAX_VALUE) return TruthnessUtils.TRUE_TRUTHNESS;
        }
        return TruthnessUtils.buildScaledTruthness(DistanceHelper.H_NOT_NULL, maxPatternSimilarity);
    }

    /**
     * H_SINTER(key1, key2, ..., db) =
     *   andAggregation(
     *     H_key_match(key1, db),
     *     H_key_match(key2, db),
     *     ...,
     *     H_non_empty_intersection(db[key1], db[key2], ...)
     *   )
     *
     * Note: type check (H_SMEMBERS) is not needed here since RedisHandler
     * pre-filters to SET keys only before reaching the calculator.
     */
    private Truthness hSinter(List<String> keys, Map<String, RedisValueData> db) {
        if (db.isEmpty() || keys.isEmpty()) {
            return TruthnessUtils.FALSE_TRUTHNESS;
        }

        List<Truthness> components = new ArrayList<>();
        List<RedisValueData> sets = new ArrayList<>();

        for (String key : keys) {
            components.add(hKeyMatch(key, db));
            sets.add(db.get(key));
        }
        components.add(hNonEmptyIntersection(sets));

        return TruthnessUtils.buildAndAggregationTruthness(
                components.toArray(new Truthness[0])
        );
    }

    /**
     * H_non_empty_intersection(set1, set2, ...) =
     *   IF any set is nil or empty THEN C_FALSE
     *   ELSE IF maxOfTrue == 1 THEN TRUE_C
     *   ELSE scaleTrue(C, maxOfTrue)
     *   where maxOfTrue = max{
     *     andAggregation(H_contains(v,set1), H_contains(v,set2), ...).ofTrue
     *     | v in set1 U set2 U ...
     *   }
     */
    private Truthness hNonEmptyIntersection(List<RedisValueData> sets) {
        Set<String> allMembers = new HashSet<>();
        for (RedisValueData s : sets) {
            if (s == null || s.getMembers() == null) {
                return TruthnessUtils.FALSE_TRUTHNESS;
            }
            allMembers.addAll(s.getMembers());
        }

        if (allMembers.isEmpty()) {
            return TruthnessUtils.FALSE_TRUTHNESS;
        }

        double maxOfTrue = H_MIN_VALUE;
        for (String value : allMembers) {
            List<Truthness> containments = new ArrayList<>();
            for (RedisValueData set : sets) {
                containments.add(hContains(value, set.getMembers()));
            }
            double ofTrue = TruthnessUtils.buildAndAggregationTruthness(
                    containments.toArray(new Truthness[0])
            ).getOfTrue();
            maxOfTrue = Math.max(maxOfTrue, ofTrue);
            if (maxOfTrue == H_MAX_VALUE) return TruthnessUtils.TRUE_TRUTHNESS;
        }
        return TruthnessUtils.buildScaledTruthness(DistanceHelper.H_NOT_NULL, maxOfTrue);
    }

    /**
     * H_contains(value, {value1, value2, ...}) =
     *   IF set is empty THEN C_FALSE
     *   ELSE scaleTrue(C, orAggregation(
     *     getStringEquals(value, value1),
     *     getStringEquals(value, value2),
     *     ...
     *   ).ofTrue)
     */
    private Truthness hContains(String value, Set<String> members) {
        List<Truthness> equalities = new ArrayList<>();
        for (String member : members) {
            Truthness eq = TruthnessUtils.getStringEqualityTruthness(value, member);
            if (taintHandler != null) {
                taintHandler.handleTaintForStringEquals(value, member, false);
            }
            equalities.add(eq);
            if (eq.isTrue()) return TruthnessUtils.TRUE_TRUTHNESS;
        }

        double orOfTrue = TruthnessUtils.buildOrAggregationTruthness(
                equalities.toArray(new Truthness[0])
        ).getOfTrue();

        if (orOfTrue == H_MAX_VALUE) {
            return TruthnessUtils.TRUE_TRUTHNESS;
        } else {
            return TruthnessUtils.buildScaledTruthness(DistanceHelper.H_NOT_NULL, orOfTrue);
        }
    }

    /**
     * H_FT_SEARCH(index, query, db) =
     *   andAggregation(
     *     H_index_has_candidates(index, db),
     *     H_ft_condition(query, index, db))
     *
     * The index name itself is not needed here: RedisHandler already pre-filters db down to the
     * candidate documents of that index (HASH keys matching one of its declared prefixes) before
     * reaching the calculator.
     */
    private Truthness hFtSearch(String query, Map<String, RedisValueData> candidates) {
        return TruthnessUtils.buildAndAggregationTruthness(
                hIndexHasCandidates(candidates),
                hFtCondition(query, candidates)
        );
    }

    /**
     * H_index_has_candidates(index, db) = getTruthnessToEmpty(#candidateDocuments(index, db)).invert()
     */
    private Truthness hIndexHasCandidates(Map<String, RedisValueData> candidates) {
        return TruthnessUtils.getTruthnessToEmpty(candidates.size()).invert();
    }

    /**
     * H_ft_condition(query, index, db) =
     *   LET candidates = candidateDocuments(index, db)
     *   IN  IF candidates is empty
     *       THEN C_FALSE
     *       ELSE
     *           LET filters = parseFtQuery(query)
     *           LET maxOfTrue = max{ H_ft_query(filters, doc).ofTrue | doc in candidates }
     *           IN  IF maxOfTrue == 1
     *               THEN TRUE_C
     *               ELSE scaleTrue(C, maxOfTrue)
     */
    private Truthness hFtCondition(String query, Map<String, RedisValueData> candidates) {
        if (candidates.isEmpty()) {
            return TruthnessUtils.FALSE_TRUTHNESS;
        }

        List<RedisSearchFilter> filters;
        try {
            filters = RedisSearchQueryParser.parse(query);
        } catch (IllegalArgumentException e) {
            SimpleLogger.uniqueWarn("Invalid or unsupported Redis search query: " + query);
            return TruthnessUtils.FALSE_TRUTHNESS;
        }

        double maxOfTrue = H_MIN_VALUE;
        for (RedisValueData doc : candidates.values()) {
            double ofTrue = hFtQuery(filters, doc).getOfTrue();
            maxOfTrue = Math.max(maxOfTrue, ofTrue);
            if (maxOfTrue == H_MAX_VALUE) return TruthnessUtils.TRUE_TRUTHNESS;
        }
        return TruthnessUtils.buildScaledTruthness(DistanceHelper.H_NOT_NULL, maxOfTrue);
    }

    /**
     * H_ft_query(filters, doc) =
     *   IF filters is empty                 // query was "*"
     *   THEN TRUE_C
     *   ELSE andAggregation({ H_ft_filter(f, doc) | f in filters })
     */
    private Truthness hFtQuery(List<RedisSearchFilter> filters, RedisValueData doc) {
        if (filters.isEmpty()) {
            return TruthnessUtils.TRUE_TRUTHNESS;
        }

        List<Truthness> components = new ArrayList<>();
        for (RedisSearchFilter filter : filters) {
            components.add(hFtFilter(filter, doc));
        }
        return TruthnessUtils.buildAndAggregationTruthness(components.toArray(new Truthness[0]));
    }

    private Truthness hFtFilter(RedisSearchFilter filter, RedisValueData doc) {
        if (filter instanceof RedisSearchTagFilter) {
            return hTagFilter((RedisSearchTagFilter) filter, doc);
        }
        if (filter instanceof RedisSearchNumericFilter) {
            return hNumericFilter((RedisSearchNumericFilter) filter, doc);
        }
        if (filter instanceof RedisSearchTextFilter) {
            return hTextFilter((RedisSearchTextFilter) filter, doc);
        }
        throw new IllegalArgumentException("Unsupported Redis search filter type: " + filter.getClass());
    }

    /**
     * H_ft_filter(TagFilter(field, values), doc) =
     *   IF doc = nil OR field not in fields(doc)
     *   THEN C_FALSE
     *   ELSE orAggregation({ getStringEquals(doc[field], v) | v in values })
     */
    private Truthness hTagFilter(RedisSearchTagFilter filter, RedisValueData doc) {
        String value = fieldValue(doc, filter.getFieldName());
        if (value == null) {
            return TruthnessUtils.FALSE_TRUTHNESS;
        }

        List<Truthness> equalities = new ArrayList<>();
        for (String candidate : filter.getValues()) {
            Truthness eq = TruthnessUtils.getStringEqualityTruthness(value, candidate);
            if (taintHandler != null) {
                taintHandler.handleTaintForStringEquals(value, candidate, false);
            }
            equalities.add(eq);
        }
        return TruthnessUtils.buildOrAggregationTruthness(equalities.toArray(new Truthness[0]));
    }

    /**
     * H_ft_filter(NumericFilter(field, min, max), doc) =
     *   IF doc = nil OR field not in fields(doc) OR doc[field] is not numeric
     *   THEN C_FALSE
     *   ELSE H_in_range(toDouble(doc[field]), min, max)
     */
    private Truthness hNumericFilter(RedisSearchNumericFilter filter, RedisValueData doc) {
        String value = fieldValue(doc, filter.getFieldName());
        if (value == null) {
            return TruthnessUtils.FALSE_TRUTHNESS;
        }

        double numericValue;
        try {
            numericValue = Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return TruthnessUtils.FALSE_TRUTHNESS;
        }

        return hInRange(numericValue, filter.getMin(), filter.getMax());
    }

    /**
     * H_in_range(value, min, max) =
     *   andAggregation(
     *       getLessThanTruthness(value, min).invert(),     // min <= value
     *       getLessThanTruthness(max, value).invert())     // value <= max
     *
     * RediSearch's "[min max]" is inclusive on both ends, so a strict less-than would report a
     * non-zero distance for a value sitting exactly on a bound even though the query matches it.
     */
    private Truthness hInRange(double value, double min, double max) {
        return TruthnessUtils.buildAndAggregationTruthness(
                TruthnessUtils.getLessThanTruthness(value, min).invert(),
                TruthnessUtils.getLessThanTruthness(max, value).invert()
        );
    }

    /**
     * H_ft_filter(TextFilter(field, term), doc) =
     *   LET regex = textTermToRegex(term)
     *   IN  IF field = ANY
     *       THEN orAggregation({ H_text_match(regex, doc[f]) | f in fields(doc) })
     *       ELSE
     *           IF doc = nil OR field not in fields(doc)
     *           THEN C_FALSE
     *           ELSE H_text_match(regex, doc[field])
     */
    private Truthness hTextFilter(RedisSearchTextFilter filter, RedisValueData doc) {
        String word = textTermWord(filter.getTerm());

        if (filter.getFieldName() == null) {
            if (doc == null || doc.getFields() == null || doc.getFields().isEmpty()) {
                return TruthnessUtils.FALSE_TRUTHNESS;
            }
            List<Truthness> matches = new ArrayList<>();
            for (String value : doc.getFields().values()) {
                matches.add(hTextMatch(word, value));
            }
            return TruthnessUtils.buildOrAggregationTruthness(matches.toArray(new Truthness[0]));
        }

        String value = fieldValue(doc, filter.getFieldName());
        if (value == null) {
            return TruthnessUtils.FALSE_TRUTHNESS;
        }
        return hTextMatch(word, value);
    }

    /**
     * H_text_match(word, value) =
     *   IF value is not textual
     *   THEN C_FALSE
     *   ELSE
     *       LET regex = textTermToRegex(word)
     *       LET similarity = 1 - normalizeValue(regexDistance(value, regex))
     *       IN  IF similarity == 1
     *           THEN TRUE_C
     *           ELSE scaleTrue(C, similarity)
     *
     * Besides the regex, the bare word is also reported to the taint handler: the regex wraps it
     * in ".*", so on its own it would no longer be recognized as an input value of the request.
     */
    private Truthness hTextMatch(String word, String value) {
        if (value == null) {
            return TruthnessUtils.FALSE_TRUTHNESS;
        }

        String regex = textTermToRegex(word);
        double similarity = H_MAX_VALUE - TruthnessUtils.normalizeValue(
                RegexDistanceUtils.getStandardDistance(value, regex));
        if (taintHandler != null) {
            taintHandler.handleTaintForRegex(value, regex);
            taintHandler.handleTaintForStringEquals(word, value, true);
        }

        if (similarity == H_MAX_VALUE) {
            return TruthnessUtils.TRUE_TRUTHNESS;
        }
        return TruthnessUtils.buildScaledTruthness(DistanceHelper.H_NOT_NULL, similarity);
    }

    /**
     * word(term) = term ends with "*" ? term without trailing "*" : term
     */
    private String textTermWord(String term) {
        return term.endsWith("*") ? term.substring(0, term.length() - 1) : term;
    }

    /**
     * textTermToRegex(word) = ".*" + escape(word) + ".*"
     *
     * The word is user input, so its regex metacharacters must be taken literally.
     */
    private String textTermToRegex(String word) {
        StringBuilder regex = new StringBuilder(".*");
        for (char c : word.toCharArray()) {
            if (".+*?()[]{}|^$\\".indexOf(c) >= 0) {
                regex.append('\\');
            }
            regex.append(c);
        }
        return regex.append(".*").toString();
    }

    private String fieldValue(RedisValueData doc, String field) {
        if (doc == null || doc.getFields() == null) {
            return null;
        }
        return doc.getFields().get(field);
    }

    /**
     * H_FT_AGGREGATE(index, query, groupByFields, db) =
     *   andAggregation(
     *       H_FT_SEARCH(index, query, db),
     *       andAggregation({ H_field_exists_anywhere(f, candidateDocuments(index, db))
     *                         | f in groupByFields }))
     *
     * When there is no GROUPBY stage, groupByFields is empty and this collapses to H_FT_SEARCH,
     * since andAggregation is undefined over an empty set of Truthness values.
     */
    private Truthness hFtAggregate(String query, List<String> groupByFields, Map<String, RedisValueData> candidates) {
        Truthness searchTruthness = hFtSearch(query, candidates);
        if (groupByFields.isEmpty()) {
            return searchTruthness;
        }

        List<Truthness> fieldExistence = new ArrayList<>();
        for (String field : groupByFields) {
            fieldExistence.add(hFieldExistsAnywhere(field, candidates));
        }
        Truthness groupByTruthness = TruthnessUtils.buildAndAggregationTruthness(fieldExistence.toArray(new Truthness[0]));

        return TruthnessUtils.buildAndAggregationTruthness(searchTruthness, groupByTruthness);
    }

    /**
     * H_field_exists_anywhere(field, candidates) =
     *   IF candidates is empty
     *   THEN C_FALSE
     *   ELSE orAggregation({ H_field_exists(field, doc) | doc in candidates })
     */
    private Truthness hFieldExistsAnywhere(String field, Map<String, RedisValueData> candidates) {
        if (candidates.isEmpty()) {
            return TruthnessUtils.FALSE_TRUTHNESS;
        }

        List<Truthness> existence = new ArrayList<>();
        for (RedisValueData doc : candidates.values()) {
            existence.add(hFieldExists(field, doc));
        }
        return TruthnessUtils.buildOrAggregationTruthness(existence.toArray(new Truthness[0]));
    }

    /**
     * H_field_exists(field, doc) =
     *   IF doc = nil OR field not in fields(doc)
     *   THEN C_FALSE
     *   ELSE TRUE_C
     */
    private Truthness hFieldExists(String field, RedisValueData doc) {
        if (doc == null || doc.getFields() == null || !doc.getFields().containsKey(field)) {
            return TruthnessUtils.FALSE_TRUTHNESS;
        }
        return TruthnessUtils.TRUE_TRUTHNESS;
    }

}