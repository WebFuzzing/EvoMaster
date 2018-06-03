package org.evomaster.core.database

import org.evomaster.core.database.schema.Column
import org.evomaster.core.database.schema.Table
import org.evomaster.core.search.Action
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.IntegerGene
import org.evomaster.core.search.gene.LongGene
import org.evomaster.core.search.gene.StringGene

/**
 *  An action executed on the database.
 *  Typically, a SQL Insertion
 */
class DbAction(
        private val table: Table,
        private val selectedColumns: Set<Column>,
        //FIXME: this should not be exposed outside this class
        computedGenes: List<Gene>? = null
) : Action {

    private val genes: List<Gene> = computedGenes ?: selectedColumns.map {
        when {
        //TODO handle all constraints and cases, eg FK
            it.type.equals("VARCHAR", ignoreCase = true) ->
                StringGene(name = it.name, minLength = 0, maxLength = it.size)
            it.type.equals("INTEGER", ignoreCase = true) ->
                IntegerGene(it.name)
            it.type.equals("LONG", ignoreCase = true) ->
                LongGene(it.name)
            else -> throw IllegalArgumentException("Cannot handle: $it")
        }
    }


    override fun getName(): String {
        return "SQL_Insert_${table.name}_${selectedColumns.joinToString("_")}"
    }

    override fun seeGenes(): List<out Gene> {
        return genes
    }

    override fun copy(): Action {
        return DbAction(table, selectedColumns, genes)
    }

    override fun shouldCountForFitnessEvaluations(): Boolean {
        return false
    }
}