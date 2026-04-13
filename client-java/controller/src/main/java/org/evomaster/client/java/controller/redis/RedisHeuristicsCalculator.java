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
    public static final double MIN_REDIS_DISTANCE = 0d;

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
     * @param redisData Redis data in a generic Map structure, where the keys are the same as in Redis and values
     *                  may contain fields or set members.
     * @return RedisDistanceWithMetrics
     */
    public RedisDistanceWithMetrics computeDistance(RedisCommand redisCommand,
                                                    RedisKeyValueStore redisData) {
        RedisCommand.RedisCommandType type = redisCommand.getType();
        try {
            switch (type) {
                case KEYS: {
                    String pattern = redisCommand.extractArgs().get(0);
                    return calculateDistanceForPattern(pattern, redisData.getData());
                }

                case EXISTS:
                case GET:
                case HGETALL:
                case SMEMBERS: {
                    String target = redisCommand.extractArgs().get(0);
                    return calculateDistanceForKeyMatch(target, redisData.getData());
                }

                case HGET: {
                    String key = redisCommand.extractArgs().get(0);
                    String field = redisCommand.extractArgs().get(1);
                    return calculateDistanceForFieldInHash(key, field, redisData.getData());
                }

                case SINTER: {
                    return calculateDistanceForIntersection(redisCommand.extractArgs(), redisData.getData());
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
     * @param redisData Redis data in a generic Map structure, where the keys are the same as in Redis and values
     *                  may contain fields or set members.
     * @return RedisDistanceWithMetrics
     */
    private RedisDistanceWithMetrics calculateDistanceForPattern(
            String pattern,
            Map<String, RedisValueData> redisData) {
        double minDist = MAX_REDIS_DISTANCE;
        int eval = 0;
        String regex;
        try {
            regex = redisPatternToRegex(pattern);
        } catch (IllegalArgumentException e) {
            SimpleLogger.uniqueWarn("Invalid Redis pattern. Cannot compute regex for: " + pattern);
            return new RedisDistanceWithMetrics(MAX_REDIS_DISTANCE, 0);
        }
        for (String key : redisData.keySet()) {
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
     * @param redisData Redis data in a generic Map structure, where the keys are the same as in Redis and values
     *                  may contain fields or set members.
     * @return RedisDistanceWithMetrics
     */
    private RedisDistanceWithMetrics calculateDistanceForKeyMatch(
            String targetKey,
            Map<String, RedisValueData> redisData
    ) {
        if (redisData.isEmpty()) {
            return new RedisDistanceWithMetrics(MAX_REDIS_DISTANCE, 0);
        }

        double minDist = MAX_REDIS_DISTANCE;
        int evaluated = 0;

        for (String key : redisData.keySet()) {
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
     * @param redisData Redis data in a generic Map structure, where the keys are the same as in Redis and values
     *                  may contain fields or set members.
     * @return RedisDistanceWithMetrics
     */
    private RedisDistanceWithMetrics calculateDistanceForFieldInHash(
            String targetKey,
            String targetField,
            Map<String, RedisValueData> redisData
    ) {
        if (redisData.isEmpty()) {
            return new RedisDistanceWithMetrics(MAX_REDIS_DISTANCE, 0);
        }

        double minDist = MAX_REDIS_DISTANCE;
        int evaluated = 0;

        for (String key : redisData.keySet()) {
            try {
                long keyDist = DistanceHelper.getLeftAlignmentDistance(targetKey, key);
                double fieldDist = calculateDistanceForField(targetField, redisData.get(key).getFields().keySet());
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
     * @param redisData Redis data in a generic Map structure, where the keys are the same as in Redis and values
     *                  may contain fields or set members.
     * @return RedisDistanceWithMetrics
     */
    private RedisDistanceWithMetrics calculateDistanceForIntersection(
            List<String> commandArgs,
            Map<String, RedisValueData> redisData
    ) {
        if (redisData == null || redisData.isEmpty()) {
            return new RedisDistanceWithMetrics(MAX_REDIS_DISTANCE, 0);
        }

        return new RedisDistanceWithMetrics(TruthnessUtils.normalizeValue(
                computeDistanceForKeysInArgs(commandArgs, redisData) +
                        computeIntersectionDistanceForArgs(commandArgs, redisData)
        ), redisData.size());
    }

    /**
     * Computes the distance for the list of keys of each one existing in Redis.
     *
     * @param commandArgs List of keys for the intersection
     * @param redisData Redis data in a generic Map structure, where the keys are the same as in Redis and values
     *                  may contain fields or set members.
     * @return RedisDistanceWithMetrics
     */
    private double computeDistanceForKeysInArgs(
            List<String> commandArgs,
            Map<String, RedisValueData> redisData
    ) {
        int numberOfCommandKeys = commandArgs.size();

        if (numberOfCommandKeys == 0) {
            throw new IllegalArgumentException("Set command encountered without arguments.");
        }

        double sum = MIN_REDIS_DISTANCE;
        for (String arg : commandArgs) {
            sum += calculateDistanceForKeyMatch(arg, redisData).getDistance();
        }
        return sum / numberOfCommandKeys;
    }

    /**
     * Computes the distance of a given intersection considering the keys present in the command args.
     *
     * @param commandArgs List of keys for the intersection
     * @param redisData Redis data in a generic Map structure, where the keys are the same as in Redis and values
     *                  may contain fields or set members.
     * @return RedisDistanceWithMetrics
     */
    private double computeIntersectionDistanceForArgs(
            List<String> commandArgs,
            Map<String, RedisValueData> redisData
    ) {

        List<Set<String>> membersInCommandArgsSets = commandArgs.stream()
                .map(key -> redisData.getOrDefault(key, new RedisValueData(new HashSet<>())).getMembers())
                .collect(Collectors.toList());

        int numberOfMembersInCommandArgsSets = membersInCommandArgsSets.size();

        if (numberOfMembersInCommandArgsSets == 0) {
            throw new IllegalArgumentException("Set command encountered without arguments.");
        }

        double total = MIN_REDIS_DISTANCE;

        Set<String> currentIntersection = null;

        for (int i = 0; i < numberOfMembersInCommandArgsSets; i++) {
            Set<String> set = membersInCommandArgsSets.get(i);

            if (i == 0) {
                currentIntersection = new HashSet<>(set);
                double initialDistance = currentIntersection.isEmpty() ? MAX_REDIS_DISTANCE : MIN_REDIS_DISTANCE;
                total += initialDistance;
            } else {
                Set<String> newIntersection = new HashSet<>(currentIntersection);
                newIntersection.retainAll(set);

                double currentIntersectionDistance = newIntersection.isEmpty()
                        ? computeSetIntersectionDistance(currentIntersection, set)
                        : MIN_REDIS_DISTANCE;

                total += currentIntersectionDistance;
                currentIntersection = newIntersection;
            }
        }

        return total / numberOfMembersInCommandArgsSets;
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
