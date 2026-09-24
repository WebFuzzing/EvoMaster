package org.evomaster.core.database.cassandra

import org.evomaster.client.java.controller.api.dto.database.execution.CassandraExecutionsDto
import org.evomaster.client.java.controller.api.dto.database.execution.CassandraFailedQuery

/**
 * Encapsulates every CQL query that matched no row during an execution, as a data-generation hint.
 */
class CassandraExecution(val failedQueries: MutableList<CassandraFailedQuery>?) {

    companion object {

        fun fromDto(dto: CassandraExecutionsDto?): CassandraExecution {
            return CassandraExecution(dto?.failedQueries?.toMutableList())
        }
    }
}