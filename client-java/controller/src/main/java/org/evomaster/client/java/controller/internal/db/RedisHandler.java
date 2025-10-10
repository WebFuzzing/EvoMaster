package org.evomaster.client.java.controller.internal.db;

import org.evomaster.client.java.controller.internal.TaintHandlerExecutionTracer;
import org.evomaster.client.java.controller.redis.RedisClient;
import org.evomaster.client.java.controller.redis.RedisHeuristicsCalculator;
import org.evomaster.client.java.instrumentation.RedisCommand;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.util.*;

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
            .forEach(redisInfo -> {
                RedisDistanceWithMetrics distanceWithMetrics = calculator.computeDistance(redisInfo, redisClient);
                evaluatedRedisCommands.add(new RedisCommandEvaluation(redisInfo, distanceWithMetrics));
        });
        operations.clear();

        return evaluatedRedisCommands;
    }

    public void setRedisClient(RedisConnectionFactory connectionFactory) {
        this.redisClient = new RedisClient(connectionFactory);
    }

}
