package org.evomaster.client.java.controller.internal.db.redis;

import org.evomaster.client.java.controller.api.dto.database.execution.RedisExecutionsDto;
import org.evomaster.client.java.controller.api.dto.database.execution.RedisFailedCommand;
import org.evomaster.client.java.controller.internal.TaintHandlerExecutionTracer;
import org.evomaster.client.java.controller.redis.RedisKeyValueStore;
import org.evomaster.client.java.controller.redis.ReflectionBasedRedisClient;
import org.evomaster.client.java.controller.redis.RedisHeuristicsCalculator;
import org.evomaster.client.java.controller.redis.RedisValueData;
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
     * Commands that have a non-zero positive distance.
     */
    private final List<RedisFailedCommand> failedCommands = new ArrayList<>();

    /**
     * Whether to calculate heuristics based on execution or not
     */
    private volatile boolean calculateHeuristics;

    /**
     * Whether to use execution's info or not
     */
    private volatile boolean extractRedisExecution;

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
        extractRedisExecution = true;
    }

    public void reset() {
        operations.clear();
        evaluatedRedisCommands.clear();
    }

    public boolean isExtractRedisExecution() {
        return extractRedisExecution;
    }

    public void setExtractRedisExecution(boolean extractRedisExecution) {
        this.extractRedisExecution = extractRedisExecution;
    }

    public RedisExecutionsDto getExecutionDto() {
        RedisExecutionsDto dto = new RedisExecutionsDto();
        dto.failedCommands.addAll(failedCommands);
        return dto;
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
                registerFailedCommand(redisCommand, distanceWithMetrics.getDistance());
            });
        operations.clear();

        return evaluatedRedisCommands;
    }

    private void registerFailedCommand(RedisCommand redisCommand, double distance) {
        if (distance > 0 &&
            redisCommand.getType().equals(RedisCommand.RedisCommandType.GET)) {
            //For this first iteration we'll only work on GET commands.
            failedCommands.add(createFailedCommand(redisCommand));
        }
    }

    private RedisFailedCommand createFailedCommand(RedisCommand redisCommand) {
        return new RedisFailedCommand(
                redisCommand.getType().getLabel().toUpperCase(),
                redisCommand.extractArgs().get(0),
                redisCommand.getType().getDataType());
    }

    private RedisDistanceWithMetrics computeDistance(RedisCommand redisCommand, ReflectionBasedRedisClient redisClient) {
        RedisCommand.RedisCommandType type = redisCommand.getType();
        try {
            switch (type) {
                case KEYS:
                case EXISTS: {
                    RedisKeyValueStore redisKeyValueStore = createRedisInfoForAllKeys(redisClient);
                    return calculator.computeDistance(redisCommand, redisKeyValueStore);
                }

                case GET: {
                    RedisKeyValueStore redisKeyValueStore = createRedisInfoForKeysByType(REDIS_STRING_TYPE, redisClient);
                    return calculator.computeDistance(redisCommand, redisKeyValueStore);
                }

                case HGET: {
                    RedisKeyValueStore redisKeyValueStore = createRedisInfoForKeysByField(redisClient);
                    return calculator.computeDistance(redisCommand, redisKeyValueStore);
                }

                case HGETALL: {
                    RedisKeyValueStore redisKeyValueStore = createRedisInfoForKeysByType(REDIS_HASH_TYPE, redisClient);
                    return calculator.computeDistance(redisCommand, redisKeyValueStore);
                }

                case SMEMBERS: {
                    RedisKeyValueStore redisKeyValueStore = createRedisInfoForKeysByType(REDIS_SET_TYPE, redisClient);
                    return calculator.computeDistance(redisCommand, redisKeyValueStore);
                }

                case SINTER: {
                    List<String> keys = redisCommand.extractArgs();
                    RedisKeyValueStore redisKeyValueStore = createRedisInfoForIntersection(keys, redisClient);
                    return calculator.computeDistance(redisCommand, redisKeyValueStore);
                }

                default:
                    return new RedisDistanceWithMetrics(MAX_REDIS_DISTANCE, 0);
            }
        } catch (Exception e) {
            SimpleLogger.warn("Could not compute distance for " + type + ": " + e.getMessage());
            return new RedisDistanceWithMetrics(MAX_REDIS_DISTANCE, 0);
        }
    }

    private RedisKeyValueStore createRedisInfoForIntersection(List<String> commandKeys, ReflectionBasedRedisClient redisClient) {
        Set<String> keySet = redisClient.getKeysByType(REDIS_SET_TYPE);

        //A Map structure is introduced here using the same keys that are stored in REDIS.
        //The value for each one, since each key represents a SET, correspond to the members of that given set.
        Map<String, RedisValueData> redisData = new HashMap<>();
        keySet.forEach(
                key -> redisData.put(key, new RedisValueData(redisClient.getSetMembers(key))
        ));
        return new RedisKeyValueStore(redisData);
    }

    private RedisKeyValueStore createRedisInfoForAllKeys(ReflectionBasedRedisClient redisClient) {
        Set<String> keys = redisClient.getAllKeys();

        //A Map structure is introduced here using the same keys that are stored in REDIS.
        //No value is needed in this case.
        Map<String, RedisValueData> redisData = new HashMap<>();
        keys.forEach(
                key -> redisData.put(key, null)
        );
        return new RedisKeyValueStore(redisData);
    }

    private RedisKeyValueStore createRedisInfoForKeysByType(String type, ReflectionBasedRedisClient redisClient) {
        Set<String> keys = redisClient.getKeysByType(type);

        //A Map structure is introduced here using the same keys that are stored in REDIS.
        //No value is needed in this case.
        Map<String, RedisValueData> redisData = new HashMap<>();
        keys.forEach(key -> redisData.put(key, null));
        return new RedisKeyValueStore(redisData);
    }

    private RedisKeyValueStore createRedisInfoForKeysByField(ReflectionBasedRedisClient redisClient) {
        Set<String> keys = redisClient.getKeysByType(REDIS_HASH_TYPE);

        //A Map structure is introduced here using the same keys that are stored in REDIS.
        //The value for each one, since each key is of type HASH, correspond to the fields stored for that given key.
        Map<String, RedisValueData> redisData = new HashMap<>();
        keys.forEach(key -> redisData.put(key, new RedisValueData(redisClient.getHashFields(key))));
        return new RedisKeyValueStore(redisData);
    }

    public void setRedisClient(ReflectionBasedRedisClient redisClient) {
        this.redisClient = redisClient;
    }
}
