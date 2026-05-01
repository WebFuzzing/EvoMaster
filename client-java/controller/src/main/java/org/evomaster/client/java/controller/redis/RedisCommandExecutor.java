package org.evomaster.client.java.controller.redis;

import org.evomaster.client.java.controller.api.dto.database.operations.RedisInsertionDto;
import org.evomaster.client.java.controller.api.dto.database.operations.RedisInsertionResultsDto;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Class used to execute Redis commands
 */
public class RedisCommandExecutor {

    /**
     * Default constructor
     */
    public RedisCommandExecutor() {}

    public static RedisInsertionResultsDto executeInsert(
            ReflectionBasedRedisClient client,
            List<RedisInsertionDto> insertions) {

        if (insertions == null || insertions.isEmpty()) {
            throw new IllegalArgumentException("No data to insert");
        }

        List<Boolean> results = new ArrayList<>(
                Collections.nCopies(insertions.size(), false));

        for (int i = 0; i < insertions.size(); i++) {
            RedisInsertionDto dto = insertions.get(i);
            try {
                client.setValue(dto.key, dto.value);
                results.set(i, true);
            } catch (Exception e) {
                throw new RuntimeException(
                        "Failed Redis insertion at index " + i +
                                " for key '" + dto.key + "': " + e.getMessage(), e);
            }
        }

        RedisInsertionResultsDto resultsDto = new RedisInsertionResultsDto();
        resultsDto.executionResults = results;
        return resultsDto;
    }
}
