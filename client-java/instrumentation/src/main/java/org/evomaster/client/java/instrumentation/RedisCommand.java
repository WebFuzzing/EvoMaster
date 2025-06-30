package org.evomaster.client.java.instrumentation;

import java.io.Serializable;

/**
 * Info related to Redis commands execution.
 */
public class RedisCommand implements Serializable {
    /**
     * Redis commands we'd like to capture. Extendable to any other command in Redis that may
     */
    public enum RedisCommandType {
        GET,
        HGET,
        HGETALL,
        SMEMBERS,
        ZRANGE
    }

    /**
     * Enumerated type of command.
     */
    private final RedisCommandType type;

    /**
     * Key used in query
     */
    private final String key;

    /**
     * Additional parameter used in some commands like HGET.
     */
    private final String subKey;

    /**
     * Type of the stored values.
     */
    private final Class<?> valueType;
    /**
     * If the operation was successfully executed
     */
    private final boolean successfullyExecuted;

    /**
     * Elapsed execution time
     */
    private final long executionTime;

    public RedisCommand(RedisCommandType type,
                        String key,
                        String subKey,
                        Class<?> valueType,
                        boolean successfullyExecuted,
                        long executionTime) {
        this.type = type;
        this.key = key;
        this.subKey = subKey;
        this.valueType = valueType;
        this.successfullyExecuted = successfullyExecuted;
        this.executionTime = executionTime;
    }

    public RedisCommandType getType() {
        return type;
    }

    public String getKey() {
        return key;
    }

    public String getSubKey() {
        return subKey;
    }

    public Class<?> getValueType() {
        return valueType;
    }

    public boolean isHashCommand() {
        return type.equals(RedisCommandType.HGET);
    }
}
