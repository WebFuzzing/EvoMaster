package org.evomaster.core.database.cassandra

import org.evomaster.core.search.action.Action
import org.evomaster.core.search.action.EnvironmentAction
import org.evomaster.core.search.gene.Gene

/**
 * An action inserting a single row into a Cassandra table, used to set up the state of the database
 * before the main actions of a test are executed.
 */
class CassandraDbAction(
    /**
     * The keyspace containing the table to insert the row into
     */
    val keyspace: String,
    /**
     * The table to insert the row into
     */
    val table: String,
    /**
     * The columns the row is composed of, ie the ones a value is generated for.
     * There is exactly one gene per column, in the same order.
     */
    val columns: List<CassandraColumn>,
    /**
     * The genes generating the value of each of the [columns], in the same order.
     * Only meant to be given when copying an existing action, so that its genes are carried over
     * instead of being built anew: when not given, one gene is built per column.
     */
    computedGenes: List<Gene>? = null
) : EnvironmentAction(listOf()) {

    private val genes: List<Gene> = (computedGenes ?: computeGenes()).also { addChildren(it) }

    init {
        if (genes.size != columns.size) {
            throw IllegalArgumentException("Mismatch between the ${columns.size} columns and the ${genes.size} genes")
        }
    }

    private fun computeGenes(): List<Gene> {
        return columns.map { CassandraColumnGeneBuilder.buildGene(it) }
    }

    override fun getName(): String {
        return "CASSANDRA_Insert_${keyspace}_${table}"
    }

    override fun seeTopGenes(): List<Gene> {
        return genes
    }

    override fun copyContent(): Action {
        return CassandraDbAction(keyspace, table, columns, genes.map(Gene::copy))
    }
}
