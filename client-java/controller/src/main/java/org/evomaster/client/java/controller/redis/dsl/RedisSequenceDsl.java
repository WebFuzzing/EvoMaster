package org.evomaster.client.java.controller.redis.dsl;

public interface RedisSequenceDsl {

    /**
     * A SET operation on the Redis database.
     *
     * @param key   the key under which the value will be stored in Redis
     * @param value the string value to store
     * @return a statement object on which the sequence can be continued or closed
     */
    RedisStatementDsl set(String key, String value);
}
