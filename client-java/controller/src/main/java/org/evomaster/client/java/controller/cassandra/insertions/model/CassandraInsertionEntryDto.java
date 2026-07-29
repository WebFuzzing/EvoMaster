package org.evomaster.client.java.controller.cassandra.insertions.model;

/**
 * A single column/value pair to insert as part of a {@link CassandraInsertionDto}.
 */
public class CassandraInsertionEntryDto {

    /**
     * Name of the column in the target Cassandra table.
     */
    public String columnName;

    /**
     * The value to insert, as it would be printed in a CQL statement.
     * This means that 5 is represented with "5", whereas "5" with "'5'".
     */
    public String printableValue;
}
