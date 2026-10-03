package org.evomaster.core.database.cassandra

import org.evomaster.client.java.controller.api.dto.database.cassandra.CassandraTableSchemaDto
import org.evomaster.core.logging.LoggingUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Builds the action inserting a row into a Cassandra table, based on the schema of that table
 * reported by the SUT driver.
 */
class CassandraInsertBuilder {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(CassandraInsertBuilder::class.java)
    }

    /**
     * @param tableSchema the schema of a table, as reported by the SUT driver
     * @return whether an insertion that could be executed can be built for such a table, ie whether
     * a value can be generated for at least one of its columns and for all of the ones composing
     * its primary key
     */
    fun canBuildInsertionFor(tableSchema: CassandraTableSchemaDto): Boolean {

        val (supported, unsupported) = partitionBySupport(tableSchema)

        return supported.isNotEmpty() && unsupported.none { isPartOfPrimaryKey(it) }
    }

    /**
     * The columns whose CQL type is not handled are left out of the insertion, as no value can be
     * generated for them. The resulting insertion is still worth executing, since the remaining
     * columns might be all that is needed, and a rejected insertion is already recorded as a failed
     * one instead of stopping the search.
     *
     * That argument does not hold when no value can be generated for any column, nor when one of
     * the skipped columns is part of the primary key, as Cassandra requires a full primary key in
     * an INSERT: in both cases the insertion could only be rejected, so none is built.
     *
     * Note that the genes of the returned action are not initialized yet, which is left to the
     * caller, as it is done for the other types of database action.
     *
     * @param tableSchema the schema of the table to insert a row into, which is also what says
     * where such a table is, ie its keyspace and its name
     * @throws IllegalArgumentException if no insertion that could be executed can be built for the
     * table, as verifiable beforehand with [canBuildInsertionFor]
     */
    fun createCassandraInsertionAction(tableSchema: CassandraTableSchemaDto): CassandraDbAction {

        val (supported, unsupported) = partitionBySupport(tableSchema)

        val qualifiedTableName = "${tableSchema.keyspaceName}.${tableSchema.tableName}"

        if (supported.isEmpty()) {
            throw IllegalArgumentException("No value can be generated for any column of" +
                    " $qualifiedTableName: ${describe(unsupported)}")
        }

        val unsupportedKeyColumns = unsupported.filter { isPartOfPrimaryKey(it) }
        if (unsupportedKeyColumns.isNotEmpty()) {
            throw IllegalArgumentException("No value can be generated for some of the columns composing" +
                    " the primary key of $qualifiedTableName: ${describe(unsupportedKeyColumns)}")
        }

        if (unsupported.isNotEmpty()) {
            LoggingUtil.uniqueWarn(
                log,
                "Cannot generate data for some columns of a Cassandra table, as their CQL type is not handled: {}",
                "$qualifiedTableName: ${describe(unsupported)}"
            )
        }

        return CassandraDbAction(tableSchema.keyspaceName, tableSchema.tableName, supported)
            .apply { forceNewTaints() }
    }

    /**
     * @return the columns a value can be generated for (first), and the ones it cannot (second)
     */
    private fun partitionBySupport(tableSchema: CassandraTableSchemaDto) =
        tableSchema.columns
            .map { CassandraColumn.fromDto(it) }
            .partition { CassandraColumnGeneBuilder.isSupported(it) }

    private fun isPartOfPrimaryKey(column: CassandraColumn) = column.isPartitionKey || column.isClusteringColumn

    private fun describe(columns: List<CassandraColumn>) = columns.joinToString(", ") { "${it.name} ${it.cqlType}" }
}