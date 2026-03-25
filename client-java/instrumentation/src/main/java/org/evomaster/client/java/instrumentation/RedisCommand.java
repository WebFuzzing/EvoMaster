package org.evomaster.client.java.instrumentation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Info related to Redis commands execution.
 */
public class RedisCommand implements Serializable {
    /**
     * Redis commands we'd like to capture. Extendable to any other command in Redis that may be of interest.
     */
    public enum RedisCommandType {
        /**
         * Removes the specified keys. A key is ignored if it does not exist.
         * Integer reply: the number of keys that were removed.
         * <a href="https://redis.io/docs/latest/commands/del/">DEL Documentation</a>
         */
        DEL("del", "mixed", false),
        /**
         * Invoke the execution of a server-side Lua script.
         * The return value depends on the script that was executed.
         * <a href="https://redis.io/docs/latest/commands/eval/">EVAL Documentation</a>
         */
        EVAL("eval", "script", false),
        /**
         * Evaluate a script from the server's cache by its SHA1 digest.
         * The return value depends on the script that was executed.
         * <a href="https://redis.io/docs/latest/commands/evalsha/">EVALSHA Documentation</a>
         */
        EVALSHA("evalsha", "script", false),
        /**
         * Returns if key exists.
         * Integer reply: the number of keys that exist from those specified as arguments.
         * <a href="https://redis.io/docs/latest/commands/exists/">EXISTS Documentation</a>
         */
        EXISTS("exists", "mixed", true),
        /**
         * Get the value of key.
         * <a href="https://redis.io/docs/latest/commands/get/">GET Documentation</a>
         */
        GET("get", "string", true),
        /**
         * Returns the value associated with field in the hash stored at key.
         * <a href="https://redis.io/docs/latest/commands/hget/">HGET Documentation</a>
         */
        HGET("hget", "hash", true),
        /**
         * Returns all fields and values of the hash stored at key.
         * <a href="https://redis.io/docs/latest/commands/hgetall/">HGETALL Documentation</a>
         */
        HGETALL("hgetall", "hash", true),
        /**
         * Increments the number stored at key by one.
         * If the key does not exist, it is set to 0 before performing the operation.
         * An error is returned if the key contains a value of the wrong type
         * or contains a string that can not be represented as integer.
         * This operation is limited to 64-bit signed integers.
         * <a href="https://redis.io/docs/latest/commands/incr/">INCR Documentation</a>
         */
        HSET("hset", "hash", false),
        /**
         * Sets the specified fields to their respective values in the hash stored at key.
         * This command overwrites the values of specified fields that exist in the hash.
         * If key doesn't exist, a new key holding a hash is created.
         * <a href="https://redis.io/docs/latest/commands/hset/">HSET Documentation</a>
         */
        INCR("incr", "string", false),
        /**
         * Returns all keys matching pattern.
         * <a href="https://redis.io/docs/latest/commands/keys/">KEYS Documentation</a>
         */
        KEYS("keys", "none", true),
        /**
         * Set key to hold the string value. If key already holds a value, it is overwritten, regardless of its type.
         * Any previous time to live associated with the key is discarded on successful SET operation.
         * <a href="https://redis.io/docs/latest/commands/set/">SET Documentation</a>
         */
        SET("set", "string", false),
        /**
         * Posts a message to the given channel.
         * Integer reply: the number of clients that the message was sent to.
         * <a href="https://redis.io/docs/latest/commands/publish/">PUBLISH Documentation</a>
         */
        PUBLISH("publish", "pubsub", false),
        /**
         * Add the specified members to the set stored at key.
         * Specified members that are already a member of this set are ignored.
         * If key does not exist, a new set is created before adding the specified members.
         * An error is returned when the value stored at key is not a set.
         * <a href="https://redis.io/docs/latest/commands/sadd/">SADD Documentation</a>
         */
        SADD("sadd", "set", false),
        /**
         * Set key to hold the string value and set key to timeout after a given number of seconds.
         * <a href="https://redis.io/docs/latest/commands/setex/">SETEX Documentation</a>
         */
        SETEX("setex", "string", false),
        /**
         * Returns the members of the set resulting from the intersection of all the given sets.
         * <a href="https://redis.io/docs/latest/commands/sinter/">SINTER Documentation</a>
         */
        SINTER("sinter", "set", true),
        /**
         * Returns all the members of the set value stored at key.
         * This has the same effect as running SINTER with one argument key.
         * <a href="https://redis.io/docs/latest/commands/smembers/">SMEMBERS Documentation</a>
         */
        SMEMBERS("smembers", "set", true),
        /**
         * Removes and returns one or more random members from the set value store at key.
         * Nil reply: if the key does not exist.
         * Bulk string reply: when called without the count argument, the removed member.
         * Array reply: when called with the count argument, a list of the removed members.
         * <a href="https://redis.io/docs/latest/commands/spop/">SPOP Documentation</a>
         */
        SPOP("spop", "set", false),
        /**
         * Remove the specified members from the set stored at key.
         * Specified members that are not a member of this set are ignored.
         * If key does not exist, it is treated as an empty set and this command returns 0.
         * An error is returned when the value stored at key is not a set.
         * <a href="https://redis.io/docs/latest/commands/srem/">SREM Documentation</a>
         */
        SREM("srem", "set", false),
        /**
         * Subscribes the client to the specified channels.
         * When successful, this command doesn't return anything.
         * Instead, for each channel, one message with the first element being the string subscribe is pushed
         * as a confirmation that the command succeeded.
         * <a href="https://redis.io/docs/latest/commands/subscribe/">SUBSCRIBE Documentation</a>
         */
        SUBSCRIBE("subscribe", "pubsub", false),
        /**
         * Unsubscribes the client from the given channels, or from all of them if none is given.
         * When successful, this command doesn't return anything.
         * Instead, for each channel, one message with the first element being the string unsubscribe is pushed
         * as a confirmation that the command succeeded.
         * <a href="https://redis.io/docs/latest/commands/unsubscribe/">UNSUBSCRIBE Documentation</a>
         */
        UNSUBSCRIBE("unsubscribe", "pubsub", false),
        /**
         * Default unregistered command value.
         */
        OTHER("other", "none", false);

        private final String label;
        private final String dataType;
        private final boolean calculateHeuristic;

        RedisCommandType(String label, String dataType, boolean shouldCalculateHeuristic) {
            this.label = label;
            this.dataType = dataType;
            this.calculateHeuristic = shouldCalculateHeuristic;
        }

        public String getLabel() {
            return label;
        }

        public String getDataType() {
            return dataType;
        }

        public boolean shouldCalculateHeuristic() {
            return calculateHeuristic;
        }
    }

    /**
     * Enumerated type of command.
     */
    private final RedisCommandType type;

    /**
     * Keys or values used in query. Keys are used in most queries. Values are used in Set commands.
     * Keys are wrapped in {@literal key<...>} while values in {@literal value<...>}
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

    public List<String> extractArgs(){
        List<String> parameters = new ArrayList<>();
        for(String arg : args){
                parameters.add(arg.substring(arg.indexOf('<')+1, arg.indexOf('>')));
        }
        return parameters;
    }

    public boolean getSuccessfullyExecuted() {
        return successfullyExecuted;
    }

    public long getExecutionTime() {
        return executionTime;
    }

    public String toString(){
        return this.getType().getLabel() + " " + String.join(" ", this.getArgs());
    }
}
