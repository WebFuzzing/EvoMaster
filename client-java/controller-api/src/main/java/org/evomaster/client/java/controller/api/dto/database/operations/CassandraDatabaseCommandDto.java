package org.evomaster.client.java.controller.api.dto.database.operations;

import java.util.ArrayList;
import java.util.List;

/**
 * Class used to execute Cassandra insertions.
 * Each item in the insertions list corresponds to a row to be inserted.
 */
public class CassandraDatabaseCommandDto {
    public List<CassandraInsertionDto> insertions = new ArrayList<>();
}
