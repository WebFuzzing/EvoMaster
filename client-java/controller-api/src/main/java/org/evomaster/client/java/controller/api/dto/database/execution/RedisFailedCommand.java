package org.evomaster.client.java.controller.api.dto.database.execution;

public class RedisFailedCommand {
    public String command;
    public String type;
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
