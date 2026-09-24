package org.evomaster.core.database.neo4j

import org.evomaster.client.java.controller.api.dto.database.execution.Neo4jExecutionsDto
import org.evomaster.client.java.controller.api.dto.database.execution.Neo4jFailedQuery

/** The MATCH queries of one action that the graph did not satisfy. */
class Neo4jExecution(val failedQueries: List<Neo4jFailedQuery>) {

    companion object {
        fun fromDto(dto: Neo4jExecutionsDto?): Neo4jExecution =
            Neo4jExecution(dto?.failedQueries ?: emptyList())
    }
}
