package org.evomaster.client.java.controller.redis;

import java.util.Set;

/**
 * This class will contain all necessary information from Redis to perform the distance calculation for a given command.
 * Hence, RedisHeuristicCalculator will be decoupled from Redis.
 * There'll be no need to call Redis to calculate distances.
 */
public class RedisInfo {
    private String key;
    private String type;
    private boolean hasField;
    private Set<String> members;

    public RedisInfo(String key) {
        this.key = key;
    }

    public RedisInfo(String key, boolean hasField) {
        this.key = key;
        this.hasField = hasField;
    }

    public RedisInfo(String key, String type, Set<String> members) {
        this.key = key;
        this.type = type;
        this.members = members;
    }

    public String getKey() {
        return key;
    }

    public String getType() {
        return type;
    }

    public boolean hasField() {
        return hasField;
    }

    public Set<String> getMembers() {
        return members;
    }
}
