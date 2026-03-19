package org.evomaster.client.java.controller.redis;

import java.util.Map;
import java.util.Set;

/**
 * This class will contain all necessary information from Redis to perform the distance calculation for a given command.
 * Hence, RedisHeuristicCalculator will be decoupled from Redis.
 * There'll be no need to call Redis to calculate distances.
 *
 * A Map structure will be used, where every key in the Map correspond to a key in Redis DB.
 * Values will depend on the type of key.
 * String keys may have a String value. Set keys will have the set members. Hash keys will have the fields.
 */
public class RedisKeyValueStore {
    private Map<String, RedisValueData> data;

    public RedisKeyValueStore(Map<String, RedisValueData> data) {
        this.data = data;
    }

    public Map<String, RedisValueData> getData() {
        return data;
    }

}
