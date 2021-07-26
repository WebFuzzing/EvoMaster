package org.evomaster.client.java.controller.api.dto.database.operations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * the execution result of {@link DatabaseCommandDto} that performs insertions
 */
public class InsertionResultsDto {

    /**
     * a map from InsertionDto id to id of auto-generated primary keys
     * in the database (if any was generated).
     */
    public Map<Long, Long> idMapping = new HashMap<>();

    /**
     * whether the insertion at the index of a sequence of SQL insertions (i.e., {@link DatabaseCommandDto#insertions})
     * executed successfully
     */
    public List<Boolean> executionResults = new ArrayList<>();
}
