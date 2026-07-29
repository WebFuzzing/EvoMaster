package org.evomaster.client.java.controller.cassandra.insertions.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Data needed to insert a single row into a Cassandra table.
 */
public class CassandraInsertionDto {

    /**
     * The keyspace containing the target table.
     */
    public String keyspaceName;

    /**
     * The target table in the keyspace.
     */
    public String tableName;

    /**
     * The columns to insert, each with its printable-literal CQL value.
     */
    public List<CassandraInsertionEntryDto> data = new ArrayList<>();
}
