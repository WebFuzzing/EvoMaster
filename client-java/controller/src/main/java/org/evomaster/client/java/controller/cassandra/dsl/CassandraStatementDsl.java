package org.evomaster.client.java.controller.cassandra.dsl;

import org.evomaster.client.java.controller.cassandra.insertions.model.CassandraInsertionDto;

import java.util.List;

public interface CassandraStatementDsl {

    /**
     * Add a value to insert
     *
     * @param columnName  name of column in the table
     * @param printableValue  the value that is going to be inserted, as
     *                        it would be printed as string.
     *                        This means that 5 is represented with "5",
     *                        whereas "5" with "'5'"
     * @return the continuation of this statement, in which more values can be added
     */
    CassandraStatementDsl d(String columnName, String printableValue);

    /**
     * Close the current statement
     * @return the sequence object on which new Cassandra commands can be added
     */
    CassandraSequenceDsl and();

    /**
     * Build the DTOs (Data Transfer Object) from this DSL,
     * closing it (ie, not usable any longer).
     * @return a list of DTOs representing all the insertion Cassandra commands defined in this DSL.
     */
    List<CassandraInsertionDto> dtos();
}
