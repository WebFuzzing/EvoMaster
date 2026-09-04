package org.evomaster.core.database.cassandra

import org.evomaster.client.java.controller.api.dto.database.operations.CassandraDatabaseCommandDto
import org.evomaster.client.java.controller.api.dto.database.operations.CassandraInsertionDto
import org.evomaster.client.java.controller.api.dto.database.operations.CassandraInsertionEntryDto

/**
 * Transforms the Cassandra insert actions of an individual into the commands to be executed on the
 * SUT side.
 */
object CassandraDbActionTransformer {

    fun transform(actions: List<CassandraDbAction>): CassandraDatabaseCommandDto {

        val insertionDtos = mutableListOf<CassandraInsertionDto>()

        for (action in actions) {

            val insertionDto = CassandraInsertionDto().apply {
                keyspaceName = action.keyspace
                tableName = action.table
            }

            action.seeTopGenes()
                .filter { it.isPrintable() }
                .forEach { gene ->
                    val entry = CassandraInsertionEntryDto().apply {
                        columnName = gene.name
                        printableValue = CassandraLiteralRenderer.toCqlLiteral(gene)
                    }
                    insertionDto.data.add(entry)
                }

            insertionDtos.add(insertionDto)
        }

        return CassandraDatabaseCommandDto().apply { this.insertions = insertionDtos }
    }
}
