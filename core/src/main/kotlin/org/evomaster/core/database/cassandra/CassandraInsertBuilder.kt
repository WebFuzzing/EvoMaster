package org.evomaster.core.database.cassandra

import org.evomaster.core.logging.LoggingUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Builds the action inserting a row into a Cassandra table, based on the description of the
 * columns of that table reported by the SUT driver.
 */
class CassandraInsertBuilder {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(CassandraInsertBuilder::class.java)
    }

    /**
     * The columns whose CQL type is not handled are left out of the insertion, as no value can be
     * generated for them. The resulting insertion is still worth executing, since the remaining
     * columns might be all that is needed, and a rejected insertion is already recorded as a failed
     * one instead of stopping the search.
     *
     * Note that the genes of the returned action are not initialized yet, which is left to the
     * caller, as it is done for the other types of database action.
     */
    fun createCassandraInsertionAction(keyspace: String, table: String, tableSchema: String): CassandraDbAction {

        val columns = CassandraTableSchemaParser.parse(tableSchema)

        val (supported, unsupported) = columns.partition { CassandraColumnGeneBuilder.isSupported(it) }

        if (unsupported.isNotEmpty()) {
            LoggingUtil.uniqueWarn(
                log,
                "Cannot generate data for some columns of $keyspace.$table, as their CQL type is not handled: {}",
                unsupported.joinToString(", ") { "${it.name} ${it.cqlType}" }
            )

            /*
                Cassandra requires a full primary key in an INSERT, so leaving out any of those
                columns means the insertion is going to be rejected.
             */
            if (unsupported.any { it.isPartitionKey || it.isClusteringColumn }) {
                LoggingUtil.uniqueWarn(
                    log,
                    "Some of those columns are part of the primary key of {}, so the insertion will fail",
                    "$keyspace.$table"
                )
            }
        }

        return CassandraDbAction(keyspace, table, supported).apply { forceNewTaints() }
    }
}
