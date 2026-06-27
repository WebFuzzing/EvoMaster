package org.evomaster.client.java.controller.api.dto.database.execution;

/**
 * Each time a Redis command is executed and returns no data, we keep track of which keys were involved,
 * as well as relevant information such as the command type.
 */
public class RedisFailedCommand {

    /**
     * Command keyword. Corresponds to a RedisCommandType label.
     */
    public String command;

    /**
     * Key involved. Could be null if the command does not have a key in the arguments. For example: KEYS (pattern).
     */
    public String key;

    /**
     * Pattern involved. It'd only apply to commands with pattern like KEYS.
     */
    public String pattern;

    /**
     * Field involved. It'd only apply to hash commands with a field like HGET.
     */
    public String field;

    public RedisFailedCommand() {}

    public RedisFailedCommand(String command, String key, String pattern, String field) {
        this.command = command;
        this.key = key;
        this.pattern = pattern;
        this.field = field;
    }
}
