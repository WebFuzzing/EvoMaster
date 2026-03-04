package org.evomaster.client.java.controller.redis;

import java.util.Map;
import java.util.Set;

/**
 * This class will contain all necessary information from Redis to perform the distance calculation for a given command.
 * Hence, RedisHeuristicCalculator will be decoupled from Redis.
 * There'll be no need to call Redis to calculate distances.
 *
 * Since this structure will be used within a Map in which the keys are the same keys as in Redis, we only need to store
 * the fields for a given hash key or the members for a given set.
 */
public class RedisValueData {
    private Map<String, String> fields;
    private Set<String> members;

    public RedisValueData(Map<String, String> fields) {
        this.fields = fields;
    }

    public RedisValueData(Set<String> members) {
        this.members = members;
    }


    public Set<String> getMembers() {
        return members;
    }

    public Map<String, String> getFields() {
        return fields;
    }
}
