package org.evomaster.client.java.controller.api.dto.database.execution;

import java.util.ArrayList;
import java.util.List;

/**
 * Each time a Redis command is executed, we keep track of those that return no data.
 * This class summarizes every failed command in a given execution.
 */
public class RedisExecutionsDto {

    public RedisExecutionsDto() {}

    public List<RedisFailedCommand> failedCommands = new ArrayList<>();
}
