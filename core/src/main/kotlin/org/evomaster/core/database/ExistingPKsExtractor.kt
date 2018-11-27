package org.evomaster.core.database

import org.evomaster.client.java.controller.api.dto.database.schema.DbSchemaDto
import org.evomaster.core.remote.service.RemoteController


class ExistingPKsExtractor(
        private val rc: RemoteController,
        private val schemaDto: DbSchemaDto
) {

    /**
     * Check current state of database.
     * For each row, create a DbAction containing only Primary Keys
     * and immutable data
     */
    fun extractExistingPKs(): List<DbAction>{

        //TODO
        return listOf()
    }

}