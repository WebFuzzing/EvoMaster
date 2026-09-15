package org.evomaster.core.database.cassandra

/**
 * A single column of a Cassandra table, as recovered from the schema description string carried by
 * a failed CQL query reported by the SUT driver.
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
)
