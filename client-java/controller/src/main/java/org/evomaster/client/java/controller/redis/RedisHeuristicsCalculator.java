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
}