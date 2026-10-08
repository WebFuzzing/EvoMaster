package org.evomaster.client.java.controller.api.dto.database.operations;

import java.util.ArrayList;
import java.util.List;

/**
 * The execution result of {@link CassandraDatabaseCommandDto} that performs insertions.
 */
public class CassandraInsertionResultsDto {

    /**
     * Whether the insertion at the index of a sequence of Cassandra insertions (i.e., {@link CassandraDatabaseCommandDto#insertions})
     * executed successfully
     */
    public List<Boolean> executionResults = new ArrayList<>();
}