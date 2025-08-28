package org.evomaster.client.java.instrumentation;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Info related to Redis commands execution.
 */
public class RedisCommand implements Serializable {
    /**
     * Redis commands we'd like to capture. Extendable to any other command in Redis that may be of interest.
     */
    public enum RedisCommandType {
        /**
         * Get the value of key.
         * <a href="https://redis.io/docs/latest/commands/get/">GET Documentation</a>
         */
        GET,
        /**
         * Returns the value associated with field in the hash stored at key.
         * <a href="https://redis.io/docs/latest/commands/hget/">HGET Documentation</a>
         */
        HGET,
        /**
         * Returns all fields and values of the hash stored at key.
         * <a href="https://redis.io/docs/latest/commands/hgetall/">HGETALL Documentation</a>
         */
        HGETALL,
        /**
         * Returns all keys matching pattern.
         * <a href="https://redis.io/docs/latest/commands/keys/">KEYS Documentation</a>
         */
        KEYS,
        /**
         * Returns the members of the set resulting from the intersection of all the given sets.
         * <a href="https://redis.io/docs/latest/commands/sinter/">SINTER Documentation</a>
         */
        SINTER,
        /**
         * Set key to hold the string value. If key already holds a value, it is overwritten, regardless of its type.
         * Any previous time to live associated with the key is discarded on successful SET operation.
         * <a href="https://redis.io/docs/latest/commands/set/">SET Documentation</a>
         */
        SET,
        /**
         * Returns all the members of the set value stored at key.
         * This has the same effect as running SINTER with one argument key.
         * <a href="https://redis.io/docs/latest/commands/smembers/">SMEMBERS Documentation</a>
         */
        SMEMBERS,
        /**
         * Default unregistered command value.
         */
        OTHER
    }

    /**
     * Enumerated type of command.
     */
    private final RedisCommandType type;

    /**
     * Keys or values used in query. Keys are used in most queries. Values are used in Set commands.
     * Keys are wrapped in key<...> while values in value<...>
     */
    private final String[] args;

    /**
     * If the operation was successfully executed.
     */
    private final boolean successfullyExecuted;

    /**
     * Elapsed execution time.
     */
    private final long executionTime;

    public RedisCommand(RedisCommandType type,
                        String[] args,
                        boolean successfullyExecuted,
                        long executionTime) {
        this.type = type;
        this.args = (args != null) ? Arrays.copyOf(args, args.length) : new String[0];
        this.successfullyExecuted = successfullyExecuted;
        this.executionTime = executionTime;
    }

    public RedisCommandType getType() {
        return type;
    }

    public String[] getArgs() {
        return args;
    }

    public boolean getSuccessfullyExecuted() {
        return successfullyExecuted;
    }

    public long getExecutionTime() {
        return executionTime;
    }

    public boolean isHashCommand() {
        return type.equals(RedisCommandType.HGET);
    }
}
