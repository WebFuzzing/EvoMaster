package org.evomaster.client.java.controller.api.dto.database.operations;

import java.util.List;

/**
 * The execution result of {@link RedisDatabaseCommandsDto} that performs insertions.
 */
public class RedisInsertionResultsDto {

    public RedisInsertionResultsDto() {}

    /**
     * Whether the insertion at the index of a sequence of Redis insertions (i.e., {@link RedisDatabaseCommandsDto#insertions})
     * executed successfully.
     */
    public List<Boolean> executionResults;
}
