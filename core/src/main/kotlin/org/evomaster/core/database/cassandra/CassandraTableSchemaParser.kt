package org.evomaster.core.database.cassandra

/**
 * Recovers the columns of a Cassandra table from the flat schema description string reported by the
 * SUT driver, ie the inverse of how [CassandraColumn]s are rendered on the client side, where each
 * column becomes "name type" optionally followed by a " PARTITION KEY" and/or " CLUSTERING" marker,
 * and columns are joined with ", ".
 */
object CassandraTableSchemaParser {

    private const val COLUMN_SEPARATOR = ','

    private const val COLUMN_NAME_TYPE_SEPARATOR = ' '

    private const val PARTITION_KEY_COLUMN_SUFFIX = " PARTITION KEY"

    private const val CLUSTERING_COLUMN_SUFFIX = " CLUSTERING"

    /**
     * @param tableSchema the description of all the columns of a table, as reported by the SUT driver
     * @return the columns described in [tableSchema], in the same order
     * @throws IllegalArgumentException if any of the described columns is malformed
     */
    fun parse(tableSchema: String): List<CassandraColumn> {

        return splitColumns(tableSchema)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { parseColumn(it) }
    }

    /**
     * Splits on the separator between columns, ignoring the separators nested inside a type
     * parameter list, as a collection type is itself rendered with them, eg "map<text, int>".
     *
     * @throws IllegalArgumentException if the type parameter lists are not balanced, as then there
     * is no telling which of the separators are the ones between columns
     */
    private fun splitColumns(tableSchema: String) =
        CqlTypeParameters.splitAtTopLevel(tableSchema, COLUMN_SEPARATOR)

    private fun parseColumn(description: String): CassandraColumn {

        var remainder = description

        /*
            The two markers are appended in this order, so they have to be peeled off in reverse.
            Both can in principle be present, as they are rendered independently of each other.
         */
        val isClusteringColumn = remainder.endsWith(CLUSTERING_COLUMN_SUFFIX)
        if (isClusteringColumn) {
            remainder = remainder.removeSuffix(CLUSTERING_COLUMN_SUFFIX)
        }
        val isPartitionKey = remainder.endsWith(PARTITION_KEY_COLUMN_SUFFIX)
        if (isPartitionKey) {
            remainder = remainder.removeSuffix(PARTITION_KEY_COLUMN_SUFFIX)
        }

        val separatorIndex = remainder.indexOf(COLUMN_NAME_TYPE_SEPARATOR)
        if (separatorIndex <= 0 || separatorIndex == remainder.length - 1) {
            throw IllegalArgumentException("Malformed description of a Cassandra column: $description")
        }

        return CassandraColumn(
            name = remainder.substring(0, separatorIndex),
            cqlType = remainder.substring(separatorIndex + 1),
            isPartitionKey = isPartitionKey,
            isClusteringColumn = isClusteringColumn
        )
    }
}
