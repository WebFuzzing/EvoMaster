package org.evomaster.core.sql

import org.evomaster.core.sql.schema.Column
import org.evomaster.core.sql.schema.Table
import org.evomaster.core.search.action.EnvironmentAction
import org.evomaster.core.search.action.Action
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.placeholder.ImmutableDataHolderGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.gene.sql.SqlPrimaryKeyGene

/**
 *  An action executed on a SQL database.
 *  Typically, a SQL Insertion operation.
 *
 *  A table is uniquely identified based on 4 properties:
 *  - the database connection (SUT could access several databases possibly on different machines)
 *  - catalog name
 *  - schema name
 *  - table name
 *
 *  Note that different databases behave differently, especially Postgres vs MySQL.
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

        /**
         * TODO need explanation
         */
        private val id: Long,

        computedGenes: List<Gene>? = null,
        /**
         * Instead of a new INSERT action, we might have "fake" actions representing
         * data already existing in the database.
         * This is very helpful when dealing with Foreign Keys.
         */
        val representExistingData: Boolean = false,

        /**
         * An id representing the connection to the database process.
         * This could be the connecting URL.
         * This is needed when dealing with multiple connection to different database processes,
         * possibly running on different machines.
         * If left null, default connection will be used (ie, assume there is only one).
         */
        val connectionId: String? = null,

        /**
         * At a high-level this represents a "catalog".
         * Ie, a "logical" database instance.
         * Note that a database process could have several logical databases.
         * We do not use the term "catalog" though as Postgres and MySQL behave differently.
         * In MySQL, can access different catalogs on same connection.
         * This is not possible on Postgres.
         * However, this latter has "schemas", which MySQL does not.
         * In other words:
         * - MySQL: this will always be empty
         * - Postgres: this would be the catalog name.
         * However, as in Postgres we cannot change catalog within same connection, the important info is in the
         * [connectionId] entry.
         * Even if we were to access 2 different catalogs in the same database process, we would need 2 different connections.
         * As such, this parameter is technically redundant.
         */
        val sealedGroupName: String? = null,

        /**
         *  In Postgres this represents a "schema", whereas it is a "catalog" for MySQL.
         *  In other words, this is used to group tables that can be indexed by this group name for disambiguation.
         */
        val openGroupName: String? = null,

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

    private val genes: List<Gene> = (computedGenes
        ?: selectedColumns.map { SqlActionGeneBuilder().buildGene(id, table, it) }
            ).also {
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
        val c = if(connectionId.isNullOrEmpty()) "" else "_$connectionId"
        val s = if(openGroupName.isNullOrEmpty()) "" else "_$openGroupName"
        val t = "_${table.name}"
        return "SQL_Insert$c$s${t}_${selectedColumns.map { it.name }.sorted().joinToString("_")}"
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
        return SqlAction(table, selectedColumns, id, genes.map(Gene::copy), representExistingData, connectionId, sealedGroupName, openGroupName)
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