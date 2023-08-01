package org.evomaster.client.java.controller.mongo.dsl;

import org.evomaster.client.java.controller.api.dto.database.operations.MongoInsertionDto;

import java.util.List;

public interface MongoStatementDsl {

    /**
     * Add a value to insert
     *
     * @param printableValue  the value that is going to be inserted, as
     *                        it would be printed as string.
     *                        This means that 5 is represented with "5",
     *                        whereas "5" with "'5'"
     * @return the continuation of this statement, in which more values can be added
     */
    MongoStatementDsl d(String printableValue);

    /**
     * Close the current statement
     * @return the sequence object on which new Mongo commands can be added
     */
    MongoSequenceDsl and();

    /**
     * Build the DTOs (Data Transfer Object) from this DSL,
     * closing it (ie, not usable any longer).
     * @return a list of DTOs representing all the insertion MONGO commands defined in this DSL.
     */
    List<MongoInsertionDto> dtos();

}
