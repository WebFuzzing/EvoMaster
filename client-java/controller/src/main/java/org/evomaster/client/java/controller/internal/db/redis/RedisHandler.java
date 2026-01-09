package org.evomaster.client.java.controller.internal.db.redis;

import org.evomaster.client.java.controller.internal.TaintHandlerExecutionTracer;
import org.evomaster.client.java.controller.redis.ReflectionBasedRedisClient;
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
     * The client must be created given both host and port for Redis DB.
     */
    private ReflectionBasedRedisClient redisClient = null;

    private final RedisHeuristicsCalculator calculator = new RedisHeuristicsCalculator(new TaintHandlerExecutionTracer());

    private static final String REDIS_HASH_TYPE = "hash";
    private static final String REDIS_SET_TYPE = "set";
    private static final String REDIS_STRING_TYPE = "string";

    public RedisHandler() {
        operations = new ArrayList<>();
        calculateHeuristics = true;
    }

    public void reset() {
        operations.clear();
        evaluatedRedisCommands.clear();
    }

    public boolean isCalculateHeuristics() {
        return calculateHeuristics;
    }

    public void setCalculateHeuristics(boolean calculateHeuristics) {
        this.calculateHeuristics = calculateHeuristics;
    }

    public void handle(RedisCommand info) {
        operations.add(info);
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

    private RedisDistanceWithMetrics computeDistance(RedisCommand redisCommand, ReflectionBasedRedisClient redisClient) {
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

    private List<RedisInfo> createRedisInfoForIntersection(List<String> keys, ReflectionBasedRedisClient redisClient) {
        List<RedisInfo> redisData = new ArrayList<>();
        keys.forEach(
                key -> redisData.add(new RedisInfo(key, redisClient.getType(key), redisClient.getSetMembers(key))
        ));
        return redisData;
    }

    private List<RedisInfo> createRedisInfoForAllKeys(ReflectionBasedRedisClient redisClient) {
        Set<String> keys = redisClient.getAllKeys();
        List<RedisInfo> redisData = new ArrayList<>();
        keys.forEach(
                key -> redisData.add(new RedisInfo(key))
        );
        return redisData;
    }

    private List<RedisInfo> createRedisInfoForKeysByType(String type, ReflectionBasedRedisClient redisClient) {
        Set<String> keys = redisClient.getKeysByType(type);
        List<RedisInfo> redisData = new ArrayList<>();
        keys.forEach(key -> redisData.add(new RedisInfo(key)));
        return redisData;
    }

    private List<RedisInfo> createRedisInfoForKeysByField(String field, ReflectionBasedRedisClient redisClient) {
        Set<String> keys = redisClient.getKeysByType(REDIS_HASH_TYPE);
        List<RedisInfo> redisData = new ArrayList<>();
        keys.forEach(key -> redisData.add(new RedisInfo(key, redisClient.getHashFields(key))));
        return redisData;
    }

    public void setRedisClient(ReflectionBasedRedisClient redisClient) {
        this.redisClient = redisClient;
    }
}
