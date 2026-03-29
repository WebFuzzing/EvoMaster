package org.evomaster.client.java.controller.api.dto.database.execution;

/**
 * Each time a Redis command is executed, we keep track of which keys were involved,
 * as well as relevant information such as the command type.
 */
public class RedisFailedCommand {

    /**
     * Command keyword. Corresponds to a RedisCommandType label.
     */
    public String command;

    /**
     * Command type. Corresponds to a RedisCommandType dataType.
     */
    public String type;

    /**
     * Key involved. Could be null if the command does not have a key in the arguments. For example: KEYS (pattern).
     */
    public String key;

    public RedisFailedCommand() {}

    public RedisFailedCommand(String command, String key, String type) {
        this.command = command;
        this.key = key;
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
