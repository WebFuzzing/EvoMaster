package org.evomaster.core.database

import org.evomaster.core.search.Action
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.SqlForeignKeyGene
import org.evomaster.core.search.gene.SqlPrimaryKeyGene
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.Lazy

object DbActionUtils {


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

    fun randomizeDbActionGenes(actions: List<DbAction>, randomness: Randomness) {
        /*
            At this point, SQL genes are particular, as they can have
            references to each other (eg Foreign Keys)
         */

        val all = actions.flatMap { it.seeGenes() }
        all.asSequence()
                .filter { it.isMutable() }
                .forEach {
                    it.randomize(randomness, false, all)
                }

        Lazy.assert{verifyForeignKeys(actions)}
    }

    private const val DEFAULT_MAX_NUMBER_OF_ATTEMPTS_TO_REPAIR_ACTIONS = 5

    /**
     * Some actions might break schema constraints
     * (such as unique columns, primary keys or foreign keys).
     * This method tries to fix each action that is broken.
     *
     * In order to do so, it starts by finding the first action with a broken gene.
     * This gene is randomize. If an action cannot be repaired after
     * <code>maxNumberOfAttemptsToRepairAnAction</code> attempts
     * (because it is not satisfiable given the current list of previous actions),
     * the remaining actions (including the one that is broken) are removed
     * from the list of actions.
     *
     * Returns true if the action list was fixed without removing any action.
     * Returns false if actions needed to be removed
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
            geneToRepair.randomize(randomness, true, previousGenes)

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
            Lazy.assert{actionIndexToRepair >= 0 && actionIndexToRepair < actions.size}
            // truncate list of actions to make them valid
            val truncatedListOfActions = actions.subList(0, actionIndexToRepair).toMutableList()
            actions.clear()
            actions.addAll(truncatedListOfActions)
            return false
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

        /*
            Key -> tableName/columnName
            Value -> raw gene value
         */
        val uniqueColumnValues = mutableMapOf<Pair<String, String>, MutableSet<String>>()

        val all = actions.flatMap { it.seeGenes() }

        for ((actionIndex, action) in actions.withIndex()) {

            if (action !is DbAction) {
                continue
            }

            val tableName = action.table.name

            action.seeGenes().forEach { g ->
                val columnName = g.name


                /*
                    TODO: following check is not taking into account that a PK could be multi-column
                 */
                if (action.table.columns.any {
                            it.name == columnName && !it.autoIncrement && (it.unique || it.primaryKey)
                        }) {

                    val key = Pair(tableName, columnName)

                    val pks = uniqueColumnValues.getOrPut(key) { mutableSetOf() }

                    /*
                        The code here cannot use Gene#ontainsSameValueAs, as the same type of
                        values could be represented with different gene structures.
                        For example, in the case of PKs, those could be regular genes, or
                        immutable ones when representing existing data in the DB.
                        So, the check for uniqueness is based on value representation...
                        but not all values can be printed... in those case we use an ad-hoc
                        string with the unique ids.
                     */
                    val value = if(g is SqlForeignKeyGene && g.isReferenceToNonPrintable(all)){
                        "FK_REFERENCE_ " + g.uniqueIdOfPrimaryKey
                    } else if((g is SqlPrimaryKeyGene && g.isReferenceToNonPrintable(all))){
                        "FK_REFERENCE_ " + (g.gene as SqlForeignKeyGene).uniqueIdOfPrimaryKey
                    } else {
                        g.getValueAsPrintableString(all)
                    }

                    if (pks.contains(value)) {
                        return Pair(g, actionIndex)
                    } else {
                        pks.add(value)
                    }
                }
            }

        }
        return Pair(null, -1)
    }
}