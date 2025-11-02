package org.evomaster.client.java.controller.internal.db.redis;

import org.evomaster.client.java.controller.internal.TaintHandlerExecutionTracer;
import org.evomaster.client.java.controller.redis.RedisClient;
import org.evomaster.client.java.controller.redis.RedisHeuristicsCalculator;
import org.evomaster.client.java.controller.redis.RedisInfo;
import org.evomaster.client.java.instrumentation.RedisCommand;
import org.evomaster.client.java.utils.SimpleLogger;

import java.util.*;

import static org.evomaster.client.java.controller.redis.RedisHeuristicsCalculator.MAX_REDIS_DISTANCE;

/**
 * Class used to act upon Redis commands executed by the SUT
 */
public class RedisHandler {

    /**
     * Info about the executed commands
     */
    private final List<RedisCommand> operations;

    /**
     * The heuristics based on the Redis execution
     */
    private final List<RedisCommandEvaluation> evaluatedRedisCommands = new ArrayList<>();

    /**
     * Whether to calculate heuristics based on execution or not
     */
    private volatile boolean calculateHeuristics;

    /**
     * Whether to use execution's info or not
     */
    private volatile boolean extractRedisExecution;

    /**
     * The client must be created through a connection factory.
     */
    private RedisClient redisClient = null;

    private final RedisHeuristicsCalculator calculator = new RedisHeuristicsCalculator(new TaintHandlerExecutionTracer());

    private static final String REDIS_HASH_TYPE = "hash";
    private static final String REDIS_SET_TYPE = "set";
    private static final String REDIS_STRING_TYPE = "string";

    public RedisHandler() {
        operations = new ArrayList<>();
        extractRedisExecution = true;
        calculateHeuristics = true;
    }

    public void reset() {
        operations.clear();
        evaluatedRedisCommands.clear();
    }

    public boolean isCalculateHeuristics() {
        return calculateHeuristics;
    }

    public boolean isExtractRedisExecution() {
        return extractRedisExecution;
    }

    public void setCalculateHeuristics(boolean calculateHeuristics) {
        this.calculateHeuristics = calculateHeuristics;
    }

    public void setExtractRedisExecution(boolean extractRedisExecution) {
        this.extractRedisExecution = extractRedisExecution;
    }

    public void handle(RedisCommand info) {
        if (extractRedisExecution) {
            operations.add(info);
        }
    }

    public List<RedisCommandEvaluation> getEvaluatedRedisCommands() {
        operations.stream()
            .filter(command -> command.getType().shouldCalculateHeuristic())
            .forEach(redisCommand -> {
                RedisDistanceWithMetrics distanceWithMetrics = computeDistance(redisCommand, redisClient);
                evaluatedRedisCommands.add(new RedisCommandEvaluation(redisCommand, distanceWithMetrics));
        });
        operations.clear();

        return evaluatedRedisCommands;
    }

    private RedisDistanceWithMetrics computeDistance(RedisCommand redisCommand, RedisClient redisClient) {
        RedisCommand.RedisCommandType type = redisCommand.getType();
        try {
            switch (type) {
                case KEYS:
                case EXISTS: {
                    List<RedisInfo> redisInfo = createRedisInfoForAllKeys(redisClient);
                    return calculator.computeDistance(redisCommand, redisInfo);
                }

                case GET: {
                    List<RedisInfo> redisInfo = createRedisInfoForKeysByType(REDIS_STRING_TYPE, redisClient);
                    return calculator.computeDistance(redisCommand, redisInfo);
                }

                case HGET: {
                    String field = redisCommand.extractArgs().get(1);
                    List<RedisInfo> redisInfo = createRedisInfoForKeysByField(field, redisClient);
                    return calculator.computeDistance(redisCommand, redisInfo);
                }

                case HGETALL: {
                    List<RedisInfo> redisInfo = createRedisInfoForKeysByType(REDIS_HASH_TYPE, redisClient);
                    return calculator.computeDistance(redisCommand, redisInfo);
                }

                case SMEMBERS: {
                    List<RedisInfo> redisInfo = createRedisInfoForKeysByType(REDIS_SET_TYPE, redisClient);
                    return calculator.computeDistance(redisCommand, redisInfo);
                }

                case SINTER: {
                    List<String> keys = redisCommand.extractArgs();
                    List<RedisInfo> redisInfo = createRedisInfoForIntersection(keys, redisClient);
                    return calculator.computeDistance(redisCommand, redisInfo);
                }

                default:
                    return new RedisDistanceWithMetrics(MAX_REDIS_DISTANCE, 0);
            }
        } catch (Exception e) {
            SimpleLogger.warn("Could not compute distance for " + type + ": " + e.getMessage());
            return new RedisDistanceWithMetrics(MAX_REDIS_DISTANCE, 0);
        }
    }

    private List<RedisInfo> createRedisInfoForIntersection(List<String> keys, RedisClient redisClient) {
        List<RedisInfo> redisData = new ArrayList<>();
        keys.forEach(
                key -> redisData.add(new RedisInfo(key, redisClient.getType(key), redisClient.getSetMembers(key))
        ));
        return redisData;
    }

    private List<RedisInfo> createRedisInfoForAllKeys(RedisClient redisClient) {
        Set<String> keys = redisClient.getAllKeys();
        List<RedisInfo> redisData = new ArrayList<>();
        keys.forEach(
                key -> redisData.add(new RedisInfo(key))
        );
        return redisData;
    }

    private List<RedisInfo> createRedisInfoForKeysByType(String type, RedisClient redisClient) {
        Set<String> keys = redisClient.getKeysByType(type);
        List<RedisInfo> redisData = new ArrayList<>();
        keys.forEach(key -> redisData.add(new RedisInfo(key)));
        return redisData;
    }

    private List<RedisInfo> createRedisInfoForKeysByField(String field, RedisClient redisClient) {
        Set<String> keys = redisClient.getKeysByType(REDIS_HASH_TYPE);
        List<RedisInfo> redisData = new ArrayList<>();
        keys.forEach(key -> redisData.add(new RedisInfo(key, redisClient.hashFieldExists(key, field))));
        return redisData;
    }

    public void setRedisClient(RedisClient redisClient) {
        this.redisClient = redisClient;
    }
}
