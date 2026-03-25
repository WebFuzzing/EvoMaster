package org.evomaster.client.java.controller.api.dto.database.execution;


import java.util.ArrayList;
import java.util.List;

public class RedisExecutionsDto {

    public RedisExecutionsDto() {}

    public List<RedisFailedCommand> failedCommands = new ArrayList<>();
}
