package org.evomaster.core.database

import org.evomaster.client.java.controller.api.dto.database.operations.DatabaseCommandDto


interface DatabaseExecutor {

    /**
     * Execute the given SQL command (in DTO format).
     * Return true if it was successful.
     */
    fun executeDatabaseCommand(dto: DatabaseCommandDto): Boolean

    /**
     * Execute a the given SQL command (in DTO format).
     * Return the result of such command, e.g., typically a QueryResultDto
     * if it was a SELECT.
     */
    fun <T> executeDatabaseCommandAndGetResults(dto: DatabaseCommandDto): T?
}