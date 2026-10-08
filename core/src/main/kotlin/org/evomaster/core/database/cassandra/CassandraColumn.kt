package org.evomaster.core.database.cassandra

import org.evomaster.client.java.controller.api.dto.database.cassandra.CassandraColumnDto

/**
 * A single column of a Cassandra table, as reported by the SUT driver in the schema of the table a
 * failed CQL query targeted.
 */
data class CassandraColumn(

    val name: String,

    /**
     * The CQL type of the column, as named in the CQL schema, eg "text", "int", "map<text, int>".
     */
    val cqlType: String,

    /**
     * Whether this column is part of the table's partition key.
     */
    val isPartitionKey: Boolean = false,

    /**
     * Whether this column is one of the table's clustering columns.
     */
    val isClusteringColumn: Boolean = false
) {

    companion object {

        /**
         * @param dto the column of a table, as reported by the SUT driver
         */
        fun fromDto(dto: CassandraColumnDto) = CassandraColumn(
            name = dto.name,
            cqlType = dto.cqlType,
            isPartitionKey = dto.isPartitionKey,
            isClusteringColumn = dto.isClusteringColumn
        )
    }
}