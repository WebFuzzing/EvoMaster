package org.evomaster.client.java.controller.api.dto.database.operations;

import java.util.ArrayList;
import java.util.List;

/**
 * Class used to execute Redis commands.
 * Each item in the insertions list corresponds to a command to be executed.
 */
public class RedisDatabaseCommandsDto {
    public List<RedisInsertionDto> insertions = new ArrayList<>();
}
