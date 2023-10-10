package org.evomaster.core.sql

import org.evomaster.core.sql.schema.Column
import org.evomaster.core.sql.schema.Table
import org.evomaster.core.search.EnvironmentAction
import org.evomaster.core.search.action.Action
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.placeholder.ImmutableDataHolderGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.gene.sql.SqlPrimaryKeyGene

/**
 *  An action executed on a SQL database.
 *  Typically, a SQL Insertion operation.
 */
class SqlAction(
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
) : EnvironmentAction(listOf()) {

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
                if (!((pk is SqlPrimaryKeyGene && pk.gene is ImmutableDataHolderGene) || pk is ImmutableDataHolderGene)) {
                    throw IllegalArgumentException("Invalid gene: ${pk.name}")
                }
            }
        }
    }

    private
    val genes: List<Gene> = (computedGenes ?: selectedColumns.map {
        SqlActionGeneBuilder().buildGene(id, table, it)
    }).also {
        // init children for DbAction
        addChildren(it)
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
            return SqlActionGeneBuilder().buildSqlTimestampGene(column.name, databaseType = column.databaseType)
        } else {
            //go for a default string
            return StringGene(name = column.name, minLength = 0, maxLength = column.size)
        }
    }

    override fun getName(): String {
        return "SQL_Insert_${table.name}_${selectedColumns.map { it.name }.sorted().joinToString("_")}"
    }

    override fun seeTopGenes(): List<out Gene> {
        return genes
    }

    fun seeGenesForInsertion(excludeColumn: List<String>) : List<out Gene>{
        if (representExistingData) throw IllegalStateException("This action is representExistingData, and seeGenesForInsertion is not applicable")
        return selectedColumns.mapIndexed { index, column ->
            if (excludeColumn.any { c-> c.equals(column.name, ignoreCase = true) }) -1
            else index
        }.filterNot { it == -1 }.map { genes[it] }
    }

    override fun copyContent(): Action {
        return SqlAction(table, selectedColumns, id, genes.map(Gene::copy), representExistingData)
    }

    fun geInsertionId(): Long {
        return this.id
    }

    //just for debugging
    fun getResolvedName() : String{
        return "SQL_Insert_${table.name}_${selectedColumns.mapIndexed { index, column -> "${column.name}:${genes.getOrNull(index).run { 
            try {
                this?.getValueAsRawString()?:"null"
            }catch (e : Exception){
                "null"
            }
        }}" }.sorted().joinToString("_")}"
    }
}