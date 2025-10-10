package org.evomaster.client.java.controller.redis;

import org.evomaster.client.java.controller.internal.db.RedisDistanceWithMetrics;
import org.evomaster.client.java.distance.heuristics.DistanceHelper;
import org.evomaster.client.java.distance.heuristics.TruthnessUtils;
import org.evomaster.client.java.instrumentation.RedisCommand;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.RegexDistanceUtils;
import org.evomaster.client.java.sql.internal.TaintHandler;
import org.evomaster.client.java.utils.SimpleLogger;

import java.util.*;

public class RedisHeuristicsCalculator {

    public static final double MAX_DISTANCE = 1d;

    private final TaintHandler taintHandler;

    public RedisHeuristicsCalculator() {
        this(null);
    }

    public RedisHeuristicsCalculator(TaintHandler taintHandler) {
        this.taintHandler = taintHandler;
    }

    /**
     * Computes the distance of a given redis command in Redis.
     * Dispatches the computation based on the command keyword (type).
     *
     * @param redisCommand Redis command
     * @param redisClient Redis Client
     * @return RedisDistanceWithMetrics
     */
    public RedisDistanceWithMetrics computeDistance(RedisCommand redisCommand, RedisClient redisClient) {
        RedisCommand.RedisCommandType type = redisCommand.getType();
        try {
            switch (type) {
                case KEYS: {
                    String pattern = redisCommand.extractArgs().get(0);
                    return calculateDistanceForPattern(pattern, redisClient.getAllKeys());
                }

                case EXISTS: {
                    String target = redisCommand.extractArgs().get(0);
                    return calculateDistanceForKeyMatch(target, redisClient.getAllKeys());
                }

                case GET: {
                    String target = redisCommand.extractArgs().get(0);
                    return calculateDistanceForKeyMatch(target, redisClient.getKeysByType("string"));
                }

                case HGET: {
                    String key = redisCommand.extractArgs().get(0);
                    String field = redisCommand.extractArgs().get(1);
                    return calculateDistanceForFieldInHash(key, field, redisClient);
                }

                case HGETALL: {
                    String key = redisCommand.extractArgs().get(0);
                    return calculateDistanceForKeyMatch(key, redisClient.getKeysByType("hash"));
                }

                case SMEMBERS: {
                    String key = redisCommand.extractArgs().get(0);
                    return calculateDistanceForKeyMatch(key, redisClient.getKeysByType("set"));
                }

                case SINTER: {
                    List<String> keys = redisCommand.extractArgs();
                    return calculateDistanceForIntersection(keys, redisClient);
                }

                default:
                    return new RedisDistanceWithMetrics(MAX_DISTANCE, 0);
            }
        } catch (Exception e) {
            SimpleLogger.warn("Could not compute distance for " + type + ": " + e.getMessage());
            return new RedisDistanceWithMetrics(MAX_DISTANCE, 0);
        }
    }

    /**
     * Computes the distance of a given pattern to the keys in Redis.
     *
     * @param pattern Pattern used to retrieve keys.
     * @param keys List of keys.
     * @return RedisDistanceWithMetrics
     */
    private RedisDistanceWithMetrics calculateDistanceForPattern(String pattern, Set<String> keys) {
        double minDist = MAX_DISTANCE;
        int eval = 0;
        for (String k : keys) {
            double d = TruthnessUtils.normalizeValue(
                    RegexDistanceUtils.getStandardDistance(k, redisPatternToRegex(pattern)));
            minDist = Math.min(minDist, d);
            eval++;
            if (d == 0) return new RedisDistanceWithMetrics(0, eval);
        }
        return new RedisDistanceWithMetrics(minDist, eval);
    }

    /**
     * Computes the distance of a given command (currently EXISTS, GET, HGETALL, SMEMBERS)
     * using the target key against the candidate keys.
     *
     * @param targetKey Primary key used in the command.
     * @param candidateKeys Keys from Redis of the same type as the command expects.
     * @return RedisDistanceWithMetrics
     */
    private RedisDistanceWithMetrics calculateDistanceForKeyMatch(
            String targetKey,
            Set<String> candidateKeys
    ) {
        if (candidateKeys.isEmpty()) {
            return new RedisDistanceWithMetrics(MAX_DISTANCE, 0);
        }

        double minDist = MAX_DISTANCE;
        int evaluated = 0;

        for (String k : candidateKeys) {
            try {
                long rawDist = DistanceHelper.getLeftAlignmentDistance(targetKey, k);
                double normDist = TruthnessUtils.normalizeValue(rawDist);
                minDist = Math.min(minDist, normDist);
                evaluated++;

                if (normDist == 0) {
                    return new RedisDistanceWithMetrics(0, evaluated);
                }
            } catch (Exception ex) {
                SimpleLogger.uniqueWarn("Failed to compute distance for key " + k + ": " + ex.getMessage());
            }
        }

        return new RedisDistanceWithMetrics(minDist, evaluated);
    }

    /**
     * Computes the distance of a given hash commend (HGET) considering both the key and the hash field.
     *
     * @param targetKey Primary key used in the command
     * @param targetField Field queried.
     * @param client Redis client
     * @return RedisDistanceWithMetrics
     */
    private RedisDistanceWithMetrics calculateDistanceForFieldInHash(
            String targetKey,
            String targetField,
            RedisClient client
    ) {
        Set<String> allKeys = client.getAllKeys();
        if (allKeys.isEmpty()) {
            return new RedisDistanceWithMetrics(MAX_DISTANCE, 0);
        }

        double minDist = MAX_DISTANCE;
        int evaluated = 0;

        for (String k : allKeys) {
            try {
                String type = client.getType(k);
                if (!"hash".equalsIgnoreCase(type)) {
                    continue;
                }

                long keyDist = DistanceHelper.getLeftAlignmentDistance(targetKey, k);

                boolean hasField = client.hashFieldExists(k, targetField);
                double fieldDist = hasField ? 0d : MAX_DISTANCE;

                double combined = TruthnessUtils.normalizeValue(keyDist + fieldDist);

                minDist = Math.min(minDist, combined);
                evaluated++;

                if (combined == 0) {
                    return new RedisDistanceWithMetrics(0, evaluated);
                }
            } catch (Exception ex) {
                SimpleLogger.uniqueWarn("Failed HGET distance on " + k + ": " + ex.getMessage());
            }
        }

        return new RedisDistanceWithMetrics(minDist, evaluated);
    }

    /**
     * Computes the distance of a given intersection considering the keys for the given sets.
     *
     * @param keys Set keys for the intersection
     * @param client Redis client
     * @return RedisDistanceWithMetrics
     */
    private RedisDistanceWithMetrics calculateDistanceForIntersection(
            List<String> keys,
            RedisClient client
    ) {
        if (keys == null || keys.isEmpty()) {
            return new RedisDistanceWithMetrics(MAX_DISTANCE, 0);
        }

        double total = 0d;
        int evaluated = 0;

        Set<String> currentIntersection = null;

        for (int i = 0; i < keys.size(); i++) {
            String k = keys.get(i);
            String type = client.getType(k);
            if (!"set".equalsIgnoreCase(type)) {
                return new RedisDistanceWithMetrics(MAX_DISTANCE, evaluated);
            }

            Set<String> set = client.getSetMembers(k);
            if (set == null) set = Collections.emptySet();

            if (i == 0) {
                currentIntersection = new HashSet<>(set);
                double d0 = currentIntersection.isEmpty() ? MAX_DISTANCE : 0d;
                total += d0;
                evaluated++;
            } else {
                Set<String> newIntersection = new HashSet<>(currentIntersection);
                newIntersection.retainAll(set);

                double di = newIntersection.isEmpty()
                        ? computeSetIntersectionDistance(currentIntersection, set)
                        : 0d;

                total += di;
                evaluated++;
                currentIntersection = newIntersection;
            }
        }

        return new RedisDistanceWithMetrics(total / keys.size(), evaluated);
    }

    /**
     * Intersection distance between two given sets.
     */
    private double computeSetIntersectionDistance(Set<String> s1, Set<String> s2) {
        if (s1.isEmpty() || s2.isEmpty()) {
            return MAX_DISTANCE;
        }

        double min = MAX_DISTANCE;
        for (String a : s1) {
            for (String b : s2) {
                long raw = DistanceHelper.getLeftAlignmentDistance(a, b);
                double norm = TruthnessUtils.normalizeValue(raw);
                min = Math.min(min, norm);
                if (min == 0) return 0;
            }
        }
        return min;
    }

    /**
     * Translates a Redis glob-style pattern into a valid Java regex pattern.
     * Supported conversions:
     * - *  →  .*
     * - ?  →  .
     * - [ae]  →  [ae]
     * - [^e]  →  [^e]
     * - [a-b]  →  [a-b]
     * Other regex metacharacters are properly escaped.
     *
     * @param redisPattern the Redis glob-style pattern (e.g., "h?llo*", "user:[0-9]*")
     * @return a valid Java regex string equivalent to the Redis pattern.
     */
    private static String redisPatternToRegex(String redisPattern) {
        if (redisPattern == null || redisPattern.isEmpty()) {
            return ".*";
        }

        StringBuilder regex = new StringBuilder();
        boolean inBrackets = false;

        for (int i = 0; i < redisPattern.length(); i++) {
            char c = redisPattern.charAt(i);

            switch (c) {
                case '*':
                    regex.append(".*");
                    break;
                case '?':
                    regex.append('.');
                    break;
                case '[':
                    inBrackets = true;
                    regex.append('[');
                    if (i + 1 < redisPattern.length() && redisPattern.charAt(i + 1) == '^') {
                        regex.append('^');
                        i++;
                    }
                    break;
                case ']':
                    if (inBrackets) {
                        regex.append(']');
                        inBrackets = false;
                    } else {
                        regex.append("\\]");
                    }
                    break;
                case '\\':
                    regex.append("\\\\");
                    break;
                default:
                    if (!inBrackets && ".+(){}|^$".indexOf(c) >= 0) {
                        regex.append('\\');
                    }
                    regex.append(c);
                    break;
            }
        }

        return "^" + regex + "$";
    }
}
