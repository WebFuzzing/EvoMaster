package org.evomaster.core.database

import org.evomaster.core.database.schema.Column
import org.evomaster.core.database.schema.Table
import org.evomaster.core.search.Action
import org.evomaster.core.search.gene.*

/**
 *  An action executed on the database.
 *  Typically, a SQL Insertion
 */
class DbAction(
        /**
         * The involved table
         */
        val table: Table,
        /**
         * Which columns we are inserting data into
         */
        val selectedColumns: Set<Column>,
        private val id: Long,
        computedGenes: List<Gene>? = null,
        /**
         * Instead of a new INSERT action, we might have "fake" actions representing
         * data already existing in the database.
         * This is very helpful when dealing with Foreign Keys.
         */
        val representExistingData: Boolean = false
) : Action {

    init {
        /*
            Existing data actions are very special, and can only contain PKs
            with immutable data.
         */
        if (representExistingData) {
            if (computedGenes == null) {
                throw IllegalArgumentException("No defined genes")
            }

            for (pk in computedGenes) {
                if (pk !is SqlPrimaryKeyGene || pk.gene !is ImmutableDataHolderGene) {
                    throw IllegalArgumentException("Invalid gene: ${pk.name}")
                }
            }
        }
    }


    private
    val genes: List<Gene> = computedGenes ?: selectedColumns.map {
        DbActionGeneBuilder().buildGene(id, table, it)
    }


    private fun handleVarBinary(column: Column): Gene {
        /*
            TODO: this is more complicated than expected, as we need
            new gene type to handle transformation to hex format
         */
        /*
            This is a nasty case, as it is a blob of binary data.
            Could be any format, and likely no constraint in the DB schema,
            where the actual constraints are in the SUT code.
            This is also what for example can be used by Hibernate to represent
            a ZoneDataTime before Java 8 support.
            A workaround for the moment is to guess a possible type/constraints
            based on the column name
         */
        if (column.name.contains("time", ignoreCase = true)) {
            return SqlTimestampGene(column.name)
        } else {
            //go for a default string
            return StringGene(name = column.name, minLength = 0, maxLength = column.size)
        }
    }

    override fun getName(): String {
        return "SQL_Insert_${table.name}_${selectedColumns.map { it.name }.joinToString("_")}"
    }

    override fun seeGenes(): List<out Gene> {
        return genes
    }

    override fun copy(): Action {
        return DbAction(table, selectedColumns, id, genes.map(Gene::copy), representExistingData)
    }

    override fun shouldCountForFitnessEvaluations(): Boolean {
        return false
    }

    fun geInsertionId(): Long {
        return this.id
    }
}