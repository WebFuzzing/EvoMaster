package org.evomaster.core.database

import org.evomaster.core.database.schema.Column
import org.evomaster.core.database.schema.ForeignKey
import org.evomaster.core.database.schema.Table
import org.evomaster.core.search.Action
import org.evomaster.core.search.gene.*

/**
 *  An action executed on the database.
 *  Typically, a SQL Insertion
 */
class DbAction(
        val table: Table,
        val selectedColumns: Set<Column>,
        private val id: Long,
        //FIXME: this should not be exposed outside this class
        computedGenes: List<Gene>? = null
) : Action {

    private val genes: List<Gene> = computedGenes ?: selectedColumns.map{

        val fk = getForeignKey(table, it)

        /*
            TODO should nullable columns be wrapped in a OptionalGene?
            Maybe not, as need special gene to represent NULL even for
            numeric values
         */

        val gene = when {
        //TODO handle all constraints and cases
            it.autoIncrement ->
                SqlAutoIncrementGene(it.name)
            fk != null ->
                SqlForeignKeyGene(it.name, id, fk.targetTable, it.nullable)
            it.type.equals("VARCHAR", ignoreCase = true) ->
                StringGene(name = it.name, minLength = 0, maxLength = it.size)
            it.type.equals("INTEGER", ignoreCase = true) ->
                IntegerGene(it.name)
            it.type.equals("LONG", ignoreCase = true) ->
                LongGene(it.name)
            it.type.equals("BIGINT", ignoreCase = true) ->
                LongGene(it.name)
            it.type.equals("BOOLEAN", ignoreCase = true) ->
                BooleanGene(it.name)
            it.type.equals("TIMESTAMP", ignoreCase = true) ->
                /*
                    TODO handle fact that TimeStamp have year limitations and possible different
                    string formats when printed
                 */
                    DateTimeGene(it.name)
//            it.type.equals("VARBINARY", ignoreCase = true) ->
//                handleVarBinary(it)
            else -> throw IllegalArgumentException("Cannot handle: $it")
        }

        if(it.primaryKey) {
            SqlPrimaryKeyGene(it.name, table.name, gene, id)
        } else {
            gene
        }
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
        if(column.name.contains("time", ignoreCase = true)){
            return DateTimeGene(column.name)
        } else {
            //go for a default string
            return StringGene(name = column.name, minLength = 0, maxLength =  column.size)
        }
    }

    private fun getForeignKey(table: Table, column: Column) : ForeignKey?{

        //TODO: what if a column is part of more than 1 FK? is that even possible?

        return table.foreignKeys.find { it.sourceColumns.contains(column) }
    }


    override fun getName(): String {
        return "SQL_Insert_${table.name}_${selectedColumns.joinToString("_")}"
    }

    override fun seeGenes(): List<out Gene> {
        return genes
    }

    override fun copy(): Action {
        return DbAction(table, selectedColumns, id, genes)
    }

    override fun shouldCountForFitnessEvaluations(): Boolean {
        return false
    }
}