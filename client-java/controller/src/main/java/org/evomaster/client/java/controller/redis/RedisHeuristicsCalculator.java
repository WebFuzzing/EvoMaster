package org.evomaster.client.java.controller.redis;

import org.evomaster.client.java.controller.internal.db.redis.RedisDistanceWithMetrics;
import org.evomaster.client.java.distance.heuristics.DistanceHelper;
import org.evomaster.client.java.distance.heuristics.TruthnessUtils;
import org.evomaster.client.java.instrumentation.RedisCommand;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.RegexDistanceUtils;
import org.evomaster.client.java.sql.internal.TaintHandler;
import org.evomaster.client.java.utils.SimpleLogger;

import java.util.*;
import java.util.stream.Collectors;

import static org.evomaster.client.java.controller.redis.RedisUtils.redisPatternToRegex;

public class RedisHeuristicsCalculator {

    public static final double MAX_REDIS_DISTANCE = 1d;

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
     * @param redisCommand Redis command.
     * @param redisValueData Redis data in a generic structure.
     * @return RedisDistanceWithMetrics
     */
    public RedisDistanceWithMetrics computeDistance(RedisCommand redisCommand,
                                                    Map<String, RedisValueData> redisValueData) {
        RedisCommand.RedisCommandType type = redisCommand.getType();
        try {
            switch (type) {
                case KEYS: {
                    String pattern = redisCommand.extractArgs().get(0);
                    return calculateDistanceForPattern(pattern, redisValueData);
                }

                case EXISTS:
                case GET:
                case HGETALL:
                case SMEMBERS: {
                    String target = redisCommand.extractArgs().get(0);
                    return calculateDistanceForKeyMatch(target, redisValueData);
                }

                case HGET: {
                    String key = redisCommand.extractArgs().get(0);
                    String field = redisCommand.extractArgs().get(1);
                    return calculateDistanceForFieldInHash(key, field, redisValueData);
                }

                case SINTER: {
                    return calculateDistanceForIntersection(redisCommand.extractArgs(), redisValueData);
                }

                default:
                    return new RedisDistanceWithMetrics(MAX_REDIS_DISTANCE, 0);
            }
        } catch (Exception e) {
            SimpleLogger.warn("Could not compute distance for " + type + ": " + e.getMessage());
            return new RedisDistanceWithMetrics(MAX_REDIS_DISTANCE, 0);
        }
    }

    /**
     * Computes the distance of a given pattern to the keys in Redis.
     *
     * @param pattern Pattern used to retrieve keys.
     * @param keys List of keys.
     * @return RedisDistanceWithMetrics
     */
    private RedisDistanceWithMetrics calculateDistanceForPattern(
            String pattern,
            Map<String, RedisValueData> keys) {
        double minDist = MAX_REDIS_DISTANCE;
        int eval = 0;
        String regex;
        try {
            regex = redisPatternToRegex(pattern);
        } catch (IllegalArgumentException e) {
            SimpleLogger.uniqueWarn("Invalid Redis pattern. Cannot compute regex for: " + pattern);
            return new RedisDistanceWithMetrics(MAX_REDIS_DISTANCE, 0);
        }
        for (String key : keys.keySet()) {
            double d = TruthnessUtils.normalizeValue(
                    RegexDistanceUtils.getStandardDistance(key, regex));
            if (taintHandler != null) {
                taintHandler.handleTaintForRegex(key, regex);
            }
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
            Map<String, RedisValueData> candidateKeys
    ) {
        if (candidateKeys.isEmpty()) {
            return new RedisDistanceWithMetrics(MAX_REDIS_DISTANCE, 0);
        }

        double minDist = MAX_REDIS_DISTANCE;
        int evaluated = 0;

        for (String key : candidateKeys.keySet()) {
            try {
                long rawDist = DistanceHelper.getLeftAlignmentDistance(targetKey, key);
                double normDist = TruthnessUtils.normalizeValue(rawDist);
                if (taintHandler != null) {
                    taintHandler.handleTaintForStringEquals(targetKey, key, false);
                }
                minDist = Math.min(minDist, normDist);
                evaluated++;

                if (normDist == 0) {
                    return new RedisDistanceWithMetrics(0, evaluated);
                }
            } catch (Exception ex) {
                SimpleLogger.uniqueWarn("Failed to compute distance for key " + key + ": " + ex.getMessage());
            }
        }

        return new RedisDistanceWithMetrics(minDist, evaluated);
    }

    /**
     * Computes the distance of a given hash commend (HGET) considering both the key and the hash field.
     *
     * @param targetKey Primary key used in the command
     * @param keys Redis data
     * @return RedisDistanceWithMetrics
     */
    private RedisDistanceWithMetrics calculateDistanceForFieldInHash(
            String targetKey,
            String targetField,
            Map<String, RedisValueData> keys
    ) {
        if (keys.isEmpty()) {
            return new RedisDistanceWithMetrics(MAX_REDIS_DISTANCE, 0);
        }

        double minDist = MAX_REDIS_DISTANCE;
        int evaluated = 0;

        for (String key : keys.keySet()) {
            try {
                long keyDist = DistanceHelper.getLeftAlignmentDistance(targetKey, key);
                double fieldDist = calculateDistanceForField(targetField, keys.get(key).getFields().keySet());
                double combined = TruthnessUtils.normalizeValue(keyDist + fieldDist);
                if (taintHandler != null) {
                    taintHandler.handleTaintForStringEquals(targetKey, key, false);
                }
                minDist = Math.min(minDist, combined);
                evaluated++;

                if (combined == 0) {
                    return new RedisDistanceWithMetrics(0, evaluated);
                }
            } catch (Exception ex) {
                SimpleLogger.uniqueWarn("Failed HGET distance on " + key + ": " + ex.getMessage());
            }
        }

        return new RedisDistanceWithMetrics(minDist, evaluated);
    }

    /**
     * Computes the distance of target field to each field in hash.
     *
     * @param targetField Field searched in query.
     * @param fields Fields in hash.
     * @return double
     */
    private double calculateDistanceForField(String targetField, Set<String> fields) {
        if (fields.isEmpty()) {
            return Double.MAX_VALUE;
        }

        double minDist = Double.MAX_VALUE;

        for (String field : fields) {
            try {
                long fieldDist = DistanceHelper.getLeftAlignmentDistance(targetField, field);
                if (taintHandler != null) {
                    taintHandler.handleTaintForStringEquals(targetField, field, false);
                }
                minDist = Math.min(minDist, fieldDist);
            } catch (Exception ex) {
                SimpleLogger.uniqueWarn("Failed FIELD distance on " + targetField + ": " + ex.getMessage());
            }
        }
        return minDist;
    }

    /**
     * Computes the distance of a given intersection considering the keys for the given sets.
     * Distance would be a function considering whether the keys are valid sets existing in Redis
     * and whether the successive intersections return elements in common.
     *
     * @param commandArgs List of keys for the intersection
     * @param storedKeys Set keys stored in Redis
     * @return RedisDistanceWithMetrics
     */
    private RedisDistanceWithMetrics calculateDistanceForIntersection(
            List<String> commandArgs,
            Map<String, RedisValueData> storedKeys
    ) {
        if (storedKeys == null || storedKeys.isEmpty()) {
            return new RedisDistanceWithMetrics(MAX_REDIS_DISTANCE, 0);
        }

        return new RedisDistanceWithMetrics(TruthnessUtils.normalizeValue(
                computeDistanceForKeysInArgs(commandArgs, storedKeys) + computeIntersectionDistanceForArgs(commandArgs, storedKeys)
        ), storedKeys.size());
    }

    /**
     * Computes the distance for the list of keys of each one existing in Redis.
     *
     * @param commandArgs List of keys for the intersection
     * @param storedKeys Set keys stored in Redis
     * @return RedisDistanceWithMetrics
     */
    private double computeDistanceForKeysInArgs(
            List<String> commandArgs,
            Map<String, RedisValueData> storedKeys
    ) {
        int numberOfCommandKeys = commandArgs.size();
        double sum = 0d;
        for (String arg : commandArgs) {
            sum += calculateDistanceForKeyMatch(arg, storedKeys).getDistance();
        }
        return sum / numberOfCommandKeys;
    }

    /**
     * Computes the distance of a given intersection considering the keys present in the command args.
     *
     * @param commandArgs List of keys for the intersection
     * @param storedSets Set keys stored in Redis
     * @return RedisDistanceWithMetrics
     */
    private double computeIntersectionDistanceForArgs(
            List<String> commandArgs,
            Map<String, RedisValueData> storedSets
    ) {

        List<Set<String>> membersInCommandArgsSets = commandArgs.stream()
                .map(key -> storedSets.getOrDefault(key, new RedisValueData(new HashSet<>())).getMembers())
                .collect(Collectors.toList());

        double total = 0d;

        Set<String> currentIntersection = null;

        for (int i = 0; i < membersInCommandArgsSets.size(); i++) {
            Set<String> set = membersInCommandArgsSets.get(i);

            if (i == 0) {
                currentIntersection = new HashSet<>(set);
                double d0 = currentIntersection.isEmpty() ? MAX_REDIS_DISTANCE : 0d;
                total += d0;
            } else {
                Set<String> newIntersection = new HashSet<>(currentIntersection);
                newIntersection.retainAll(set);

                double di = newIntersection.isEmpty()
                        ? computeSetIntersectionDistance(currentIntersection, set)
                        : 0d;

                total += di;
                currentIntersection = newIntersection;
            }
        }

        return total / membersInCommandArgsSets.size();
    }

    /**
     * Intersection distance between two given sets.
     */
    private double computeSetIntersectionDistance(Set<String> s1, Set<String> s2) {
        if (s1.isEmpty() || s2.isEmpty()) {
            return MAX_REDIS_DISTANCE;
        }

        double min = MAX_REDIS_DISTANCE;
        for (String a : s1) {
            for (String b : s2) {
                long raw = DistanceHelper.getLeftAlignmentDistance(a, b);
                if (taintHandler != null) {
                    taintHandler.handleTaintForStringEquals(a, b, false);
                }
                double norm = TruthnessUtils.normalizeValue(raw);
                min = Math.min(min, norm);
                if (min == 0) return 0;
            }
        }
        return min;
    }

}
