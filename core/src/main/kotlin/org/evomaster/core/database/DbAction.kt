package org.evomaster.core.database

import org.evomaster.core.database.schema.Column
import org.evomaster.core.database.schema.ColumnDataType.*
import org.evomaster.core.database.schema.ForeignKey
import org.evomaster.core.database.schema.Table
import org.evomaster.core.search.Action
import org.evomaster.core.search.gene.*
import org.evomaster.core.search.service.Randomness

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

    companion object {

        fun verifyForeignKeys(actions: List<DbAction>): Boolean {

            val all = actions.flatMap { it.seeGenes() }

            for (i in 1 until actions.size) {

                val previous = actions.subList(0, i)

                actions[i].seeGenes().asSequence()
                        .flatMap { it.flatView().asSequence() }
                        .filterIsInstance<SqlForeignKeyGene>()
                        .filter { it.isReferenceToNonPrintable(all) }
                        .map { it.uniqueIdOfPrimaryKey }
                        .forEach {
                            val id = it
                            val match = previous.asSequence()
                                    .flatMap { it.seeGenes().asSequence() }
                                    .filterIsInstance<SqlPrimaryKeyGene>()
                                    .any { it.uniqueId == id }

                            if (!match) {
                                return false
                            }
                        }
            }
            return true
        }

        //FIXME need refactoring
        fun randomizeDbActionGenes(actions: List<DbAction>, randomness: Randomness) {
            /*
                At this point, SQL genes are particular, as they can have
                references to each other (eg Foreign Keys)

                FIXME: refactoring to put such concept at higher level directly in Gene.
                And, in any case, shouldn't something specific just for Rest
             */

            val all = actions.flatMap { it.seeGenes() }
            all.asSequence()
                    .filter { it.isMutable() }
                    .forEach {
                        if (it is SqlPrimaryKeyGene) {
                            val g = it.gene
                            if (g is SqlForeignKeyGene) {
                                g.randomize(randomness, false, all)
                            } else {
                                it.randomize(randomness, false)
                            }
                        } else if (it is SqlForeignKeyGene) {
                            it.randomize(randomness, false, all)
                        } else {
                            it.randomize(randomness, false)
                        }
                    }

            if (javaClass.desiredAssertionStatus()) {
                //TODO refactor if/when Kotlin will support lazy asserts
                assert(DbAction.verifyForeignKeys(actions))
            }

        }

        private val DEFAULT_MAX_NUMBER_OF_ATTEMPTS_TO_REPAIR_ACTIONS = 5

        /**
         * Some actions might break schema constraints
         * (such as unique columns or primary keys).
         * This method tries to fix each unique column that is broken
         *
         * The repair algorithm first tries to modify genes.
         * If it is unable to do, it starts removing Db Actions
         *
         * Returns true if the action list was fixed without truncating it.
         * Returns false if the list needed to be truncated
         */
        fun repairBrokenDbActionsList(actions: MutableList<DbAction>,
                                      randomness: Randomness,
                                      maxNumberOfAttemptsToRepairAnAction: Int = DEFAULT_MAX_NUMBER_OF_ATTEMPTS_TO_REPAIR_ACTIONS
        ): Boolean {

            if (maxNumberOfAttemptsToRepairAnAction < 0) {
                throw IllegalArgumentException("Maximum umber of attempts to fix an action should be non negative but it is: $maxNumberOfAttemptsToRepairAnAction")
            }

            var attemptCounter = 0
            var previousActionIndexToRepair = -1

            var geneToRepairAndActionIndex = findFirstOffendingGeneWithIndex(actions)
            var geneToRepair = geneToRepairAndActionIndex.first
            var actionIndexToRepair = geneToRepairAndActionIndex.second

            while (geneToRepair != null && attemptCounter < maxNumberOfAttemptsToRepairAnAction) {

                val previousGenes = actions.subList(0, geneToRepairAndActionIndex.second).flatMap { it.seeGenes() }
                randomizeGene(geneToRepair, randomness, previousGenes)

                if (actionIndexToRepair == previousActionIndexToRepair) {
                    //
                    attemptCounter++
                } else if (actionIndexToRepair > previousActionIndexToRepair) {
                    attemptCounter = 0
                    previousActionIndexToRepair = actionIndexToRepair
                } else {
                    throw IllegalStateException("Invalid last action repaired at position $previousActionIndexToRepair " +
                            " but new action to repair at position $actionIndexToRepair")
                }

                geneToRepairAndActionIndex = findFirstOffendingGeneWithIndex(actions)
                geneToRepair = geneToRepairAndActionIndex.first
                actionIndexToRepair = geneToRepairAndActionIndex.second
            }

            if (geneToRepair == null) {
                return true
            } else {
                assert(actionIndexToRepair >= 0 && actionIndexToRepair < actions.size)
                // truncate list of actions to make them valid
                val truncatedListOfActions = actions.subList(0, actionIndexToRepair).toMutableList()
                actions.clear()
                actions.addAll(truncatedListOfActions)
                return false
            }
        }

        private fun randomizeGene(gene: Gene, randomness: Randomness, previousGenes: List<Gene>) {
            when (gene) {
                is SqlForeignKeyGene -> gene.randomize(randomness, true, previousGenes)
                else ->
                    if (gene is SqlPrimaryKeyGene && gene.gene is SqlForeignKeyGene) {
                        //FIXME: this needs refactoring
                        gene.gene.randomize(randomness, true, previousGenes)
                    } else {
                        //TODO other cases
                        gene.randomize(randomness, true)
                    }
            }
        }

        /**
         * Returns true iff all action are valid wrt the schema.
         * For example
         */
        fun verifyActions(actions: List<DbAction>): Boolean {
            return verifyUniqueColumns(actions)
                    && verifyForeignKeys(actions)
        }

        /**
         * Returns true if a insertion tries to insert a repeated value
         * in a unique column
         */
        fun verifyUniqueColumns(actions: List<DbAction>): Boolean {
            val offendingGene = findFirstOffendingGeneWithIndex(actions)
            return (offendingGene.first == null)
        }

        /**
         * Returns the first offending gene found with the action index to the
         * passed list where the gene was found.
         * If no such gene is found, the function returns the tuple (-1,null).
         */
        private fun findFirstOffendingGeneWithIndex(actions: List<Action>): Pair<Gene?, Int> {
            val uniqueColumnValues = mutableMapOf<Pair<String, String>, MutableSet<Gene>>()

            for ((actionIndex, action) in actions.withIndex()) {
                if (action !is DbAction) {
                    continue
                }

                val tableName = action.table.name

                action.seeGenes().forEach { g ->
                    val columnName = g.name
                    if (action.table.columns.filter { c ->
                                c.name == columnName && !c.autoIncrement && (c.unique || c.primaryKey)
                            }.isNotEmpty()) {
                        val key = Pair(tableName, columnName)

                        val genes = uniqueColumnValues.getOrPut(key) { mutableSetOf() }

                        if (genes.filter { otherGene -> otherGene.containsSameValueAs(g) }.isNotEmpty()) {
                            return Pair(g, actionIndex)
                        } else {
                            genes.add(g)
                        }
                    }
                }

            }
            return Pair(null, -1)
        }
    }


    private
    val genes: List<Gene> = computedGenes ?: selectedColumns.map {

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

            else -> when (it.type) {
                /**
                 * BOOLEAN(1) is assumed to be a boolean/Boolean field
                 */
                BOOLEAN -> BooleanGene(it.name)
                /**
                 * TINYINT(3) is assumed to be representing a byte/Byte field
                 */
                TINYINT -> IntegerGene(it.name, min = Byte.MIN_VALUE.toInt(), max = Byte.MAX_VALUE.toInt())
                /**
                 * SMALLINT(5) is assumed as a short/Short field
                 */
                SMALLINT -> IntegerGene(it.name, min = Short.MIN_VALUE.toInt(), max = Short.MAX_VALUE.toInt())
                /**
                 * CHAR(255) is assumed to be a char/Character field.
                 * A StringGene of length 1 is used to represent the data.
                 * TODO How to discover if it is a char or a char[] of 255 elements?
                 */
                CHAR -> StringGene(name = it.name, value = "f", minLength = 0, maxLength = 1)
                /**
                 * INTEGER(10) is a int/Integer field
                 */
                INTEGER -> IntegerGene(it.name)
                /**
                 * BIGINT(19) is a long/Long field
                 */
                BIGINT -> LongGene(it.name)
                /**
                 * DOUBLE(17) is assumed to be a double/Double field
                 * TODO How to discover if the source field is a float/Float field?
                 */

                DOUBLE -> DoubleGene(it.name)
                /**
                 * VARCHAR(N) is assumed to be a String with a maximum length of N.
                 * N could be as large as Integer.MAX_VALUE
                 */
                VARCHAR -> StringGene(name = it.name, minLength = 0, maxLength = it.size)
                /**
                 * TIMESTAMP is assumed to be a Date field
                 */
                TIMESTAMP -> SqlTimestampGene(it.name)
                /**
                 * CLOB(N) stores a UNICODE document of length N
                 */
                CLOB -> StringGene(name = it.name, minLength = 0, maxLength = it.size)
                //it.type.equals("VARBINARY", ignoreCase = true) ->
                //handleVarBinary(it)

                /**
                 * REAL is identical to the floating point statement float(24).
                 * TODO How to discover if the source field is a float/Float field?
                 */
                REAL -> DoubleGene(it.name)

                /**
                 * TODO: DECIMAL precision is lower than a float gene
                 */
                DECIMAL -> FloatGene(it.name)

                else -> throw IllegalArgumentException("Cannot handle: $it")
            }

        }

        if (it.primaryKey) {
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
        if (column.name.contains("time", ignoreCase = true)) {
            return SqlTimestampGene(column.name)
        } else {
            //go for a default string
            return StringGene(name = column.name, minLength = 0, maxLength = column.size)
        }
    }

    private fun getForeignKey(table: Table, column: Column): ForeignKey? {

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
        return DbAction(table, selectedColumns, id, genes.map(Gene::copy))
    }

    override fun shouldCountForFitnessEvaluations(): Boolean {
        return false
    }

    fun geInsertionId(): Long {
        return this.id
    }
}