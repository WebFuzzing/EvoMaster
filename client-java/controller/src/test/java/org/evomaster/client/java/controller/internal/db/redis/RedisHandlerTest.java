package org.evomaster.client.java.controller.internal.db.redis;

import org.evomaster.client.java.instrumentation.RedisCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RedisHandlerTest {

    private RedisHandler handler;

    @BeforeEach
    void setup() {
        handler = new RedisHandler();
        handler.setCalculateHeuristics(true);
    }

    @Test
    void testHandleStoresCommands() {
        RedisCommand cmd = new RedisCommand(
                RedisCommand.RedisCommandType.EXISTS,
                new String[]{"user:1"},
                true,
                5
        );
        handler.handle(cmd);

        assertTrue(handler.isExtractRedisExecution());
        List<RedisCommandEvaluation> evals = handler.getEvaluatedRedisCommands();

        assertNotNull(evals);
        assertFalse(evals.isEmpty());
    }

    @Test
    void testResetClearsOperations() {
        RedisCommand cmd = new RedisCommand(
                RedisCommand.RedisCommandType.EXISTS,
                new String[]{"user:1"},
                true,
                5
        );
        handler.handle(cmd);
        handler.reset();

        List<RedisCommandEvaluation> evals = handler.getEvaluatedRedisCommands();
        assertTrue(evals.isEmpty());
    }
}