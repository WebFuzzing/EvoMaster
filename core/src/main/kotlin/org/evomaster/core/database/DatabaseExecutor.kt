package org.evomaster.core.database

import org.evomaster.client.java.controller.api.dto.database.operations.DatabaseCommandDto
import org.evomaster.client.java.controller.api.dto.database.operations.QueryResultDto


interface DatabaseExecutor {

    /**
     * Execute the given SQL command (in DTO format).
     * Return true if it was successful.
     */
    fun executeDatabaseCommand(dto: DatabaseCommandDto): Boolean

    /**
     * Execute a the given SQL command (in DTO format).
     * Return the result of such command, if any
     */
    fun executeDatabaseCommandAndGetQueryResults(dto: DatabaseCommandDto): QueryResultDto?

    fun executeDatabaseInsertionsAndGetIdMapping(dto: DatabaseCommandDto): Map<Long,Long>?
}