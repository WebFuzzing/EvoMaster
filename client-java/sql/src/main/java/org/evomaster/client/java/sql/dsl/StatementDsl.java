package org.evomaster.client.java.sql.dsl;

import org.evomaster.client.java.controller.api.dto.database.operations.InsertionDto;

import java.util.List;

public interface StatementDsl {

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
    StatementDsl d(String columnName, String printableValue);

    /**
     *
     * @param columnName  name of column in the table
     * @param insertionId   id of an insertion operation done previously.
     *                      This field represents a foreign key to that row,
     *                      where the primary key is dynamically computed by
     *                      the database (eg, auto-increment).
     * @return the continuation of this statement, in which more values can be added
     */
    StatementDsl r(String columnName, long insertionId);


    /**
     * Close the current statement
     * @return the sequence object on which new SQL commands can be added
     */
    SequenceDsl and();

    /**
     * Build the DTOs (Data Transfer Object) from this DSL,
     * closing it (ie, not usable any longer).
     * @return a list of DTOs representing all the insertion SQL commands defined in this DSL.
     */
    List<InsertionDto> dtos();

}
