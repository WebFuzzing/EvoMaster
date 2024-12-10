package org.evomaster.core.sql

import org.evomaster.client.java.controller.api.dto.database.operations.*


interface DatabaseExecutor {

    /**
     * Execute the given SQL command (in DTO format).
     * Return true if it was successful.
     */
    fun executeDatabaseCommand(dto: DatabaseCommandDto): Boolean

    /**
     * Execute a the given SQL command (in DTO format).
     * Return the result of whether it success (first) and such command (second), if any
     */
    fun executeDatabaseCommandAndGetQueryResults(dto: DatabaseCommandDto): QueryResultDto?

    /**
     * Execute a the given INSERT SQL command (in DTO format).
     * Return the result of whether it success (first) and new pks in such insertions (second), if any
     */
    fun executeDatabaseInsertionsAndGetIdMapping(dto: DatabaseCommandDto): InsertionResultsDto?

    /**
     * Execute a the given INSERT MONGO command (in DTO format).
     * Return the result of whether it success
     */
    fun executeMongoDatabaseInsertions(dto: MongoDatabaseCommandDto): MongoInsertionResultsDto?
}