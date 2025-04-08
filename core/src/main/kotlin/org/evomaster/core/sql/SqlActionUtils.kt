package org.evomaster.core.sql

import org.evomaster.core.Lazy
import org.evomaster.core.search.action.Action
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.sql.SqlForeignKeyGene
import org.evomaster.core.search.gene.sql.SqlPrimaryKeyGene
import org.evomaster.core.search.service.Randomness
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.evomaster.core.sql.schema.Table

object SqlActionUtils {

    private val log: Logger = LoggerFactory.getLogger(SqlActionUtils::class.java)

    fun verifyForeignKeys(actions: List<SqlAction>): Boolean {

        for (i in 0 until actions.size) {

            val fks = actions[i].seeTopGenes()
                    .flatMap { it.flatView() }
                    .filterIsInstance<SqlForeignKeyGene>()

            if (fks.any { !it.nullable && !it.isBound() }) {
                return false
            }

            if (i == 0) {
                if(fks.isEmpty())
                    continue
                else
                    return false
            }

            /*
                note: a row could have FK to itself... weird, but possible.
                but not sure if we should allow it
             */
            val previous = actions.subList(0, i)

            fks.filter { it.isBound() }
                    .map { it.uniqueIdOfPrimaryKey }
                    .forEach { id ->
                        val match = previous.asSequence()
                                .flatMap { it.seeTopGenes().asSequence() }
                                .filterIsInstance<SqlPrimaryKeyGene>()
                                .any { it.uniqueId == id }

                        if (!match) {
                            return false
                        }
                    }
        }

        return true
    }

    fun randomizeDbActionGenes(actions: List<SqlAction>, randomness: Randomness) {
        /*
            At this point, SQL genes are particular, as they can have
            references to each other (eg Foreign Keys)
         */

        /*
            TODO do we need this method? or shall have it as general global post-processing?
         */


        actions.forEach {
            it.randomize(randomness, false)
        }

        Lazy.assert { verifyForeignKeys(actions) }
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
    fun repairBrokenDbActionsList(actions: MutableList<SqlAction>,
                                  randomness: Randomness,
                                  maxNumberOfAttemptsToRepairAnAction: Int = DEFAULT_MAX_NUMBER_OF_ATTEMPTS_TO_REPAIR_ACTIONS
    ): Boolean {

        //FIXME: quite bit of code now would break this invariant. fix once refactor post-processing
        //if(actions.map { it.getRoot() }.toSet().size != 1){
        //    throw IllegalArgumentException("All DB actions should be mounted under same root")
        //}

        if (log.isTraceEnabled){
            log.trace("before repairBrokenDbActionsList, the actions are {}", actions.joinToString(",") { it.getResolvedName() })
        }

        if (maxNumberOfAttemptsToRepairAnAction < 0) {
            throw IllegalArgumentException("Maximum umber of attempts to fix an action should be non negative but it is: $maxNumberOfAttemptsToRepairAnAction")
        }

        var attemptCounter = 0
        var previousActionIndexToRepair = -1

        var geneToRepairAndActionIndex = findFirstOffendingGeneWithIndex(actions, randomness)
        var geneToRepair = geneToRepairAndActionIndex.first
        var actionIndexToRepair = geneToRepairAndActionIndex.second

        while (geneToRepair != null && attemptCounter < maxNumberOfAttemptsToRepairAnAction) {

            //TODO check if this will still be needed after refactoring
            val previousGenes = actions.subList(0, geneToRepairAndActionIndex.second).flatMap { it.seeTopGenes() }

            if (geneToRepair.isMutable())
                geneToRepair.randomize(randomness, true)

            if (actionIndexToRepair == previousActionIndexToRepair) {
                attemptCounter++
            } else if (actionIndexToRepair > previousActionIndexToRepair) {
                attemptCounter = 0
                previousActionIndexToRepair = actionIndexToRepair
            } else {
                throw IllegalStateException("Invalid last action repaired at position $previousActionIndexToRepair " +
                        " but new action to repair at position $actionIndexToRepair")
            }

            geneToRepairAndActionIndex = findFirstOffendingGeneWithIndex(actions, randomness)
            geneToRepair = geneToRepairAndActionIndex.first
            actionIndexToRepair = geneToRepairAndActionIndex.second
        }


        if (geneToRepair == null) {

            if (log.isTraceEnabled){
                log.trace("nothing is changed, and after repairBrokenDbActionsList, the actions are {}", actions.joinToString(",") { it.getResolvedName() })
            }

            return true
        } else {
            Lazy.assert { actionIndexToRepair >= 0 && actionIndexToRepair < actions.size }
            // truncate list of actions to make them valid
            val truncatedListOfActions = actions.subList(0, actionIndexToRepair).toMutableList()
            actions.clear()
            actions.addAll(truncatedListOfActions)

            if (log.isTraceEnabled){
                log.trace("genes are repaired ,and after repairBrokenDbActionsList, the actions are {}", actions.joinToString(",") { it.getResolvedName() })
            }

            return false
        }
    }

    /**
     * Returns true iff all action are valid wrt the schema.
     * For example
     */
    fun verifyActions(actions: List<SqlAction>): Boolean {
        return verifyUniqueColumns(actions)
                && verifyForeignKeys(actions)
                && verifyExistingDataFirst(actions)
    }

    fun checkActions(actions: List<SqlAction>){

        if(!verifyActions(actions)){
            if(!verifyUniqueColumns(actions)){
                throw IllegalStateException("Unsatisfied unique column constraints")
            }
            if(!verifyForeignKeys(actions)){
                throw IllegalStateException("Unsatisfied foreign key constraints")
            }
            if(!verifyExistingDataFirst(actions)){
                throw IllegalStateException("Unsatisfied existing data constraints")
            }
            throw IllegalStateException("Bug in EvoMaster, unhandled verification case in SQL properties")
        }
    }

    fun verifyExistingDataFirst(actions: List<SqlAction>) : Boolean{
        val startingIndex = actions.indexOfLast { it.representExistingData } + 1
        return actions.filterIndexed { i,a-> i<startingIndex && !a.representExistingData }.isEmpty()
    }

    /**
     * Returns false if a insertion tries to insert a repeated value
     * in a unique column
     */
    fun verifyUniqueColumns(actions: List<SqlAction>): Boolean {
        val offendingGene = findFirstOffendingGeneWithIndex(actions)
        return (offendingGene.first == null)
    }


    /**
     * Returns the gene and action index that does not satisfy any Table Constraint of the corresponding
     * table that the action is inserting to.
     * It also returns one of the genes involved in the constraint that is not being
     * satisfied.
     * If no such gene is found, the function returns the tuple (-1,null).
     * If randomness is provided, the returning gene is randomly selected from all the genes in the constraint
     */
    private fun checkIfTableConstraintsAreSatisfied(
        sqlAction: SqlAction,
        dbActionIndex: Int,
        sqlActions: List<SqlAction>,
        randomness: Randomness? = null
    ): Pair<Gene?, Int>? {


        val tableConstraints = sqlAction.table.tableConstraints.filter {
            it.tableName == sqlAction.table.name
        }

        for (tableConstraint in tableConstraints) {
            val previousDbActions = if (dbActionIndex==0) listOf() else sqlActions.subList(0, dbActionIndex - 1)
            val evaluator = TableConstraintEvaluator(previousDbActions)
            if (tableConstraint.accept(evaluator, sqlAction) == false) {
                // This constraint is not satisfied, collect all genes related to constraint
                val geneCollector = TableConstraintGeneCollector()
                val genes = tableConstraint.accept(geneCollector, sqlAction)
                // it is expected that at least one gene should be involved in not satisfying this

                val chosenGene = if (randomness == null) {
                    genes.first()
                } else {
                    randomness.choose(genes)
                }
                return Pair(chosenGene, dbActionIndex)
            }
        }

        // no problem found
        return null

    }

    /**
     * Returns the first offending gene found with the action index to the
     * passed list where the gene was found.
     * If no such gene is found, the function returns the tuple (-1,null).
     */
    private fun findFirstOffendingGeneWithIndex(
        actions: List<Action>,
        randomness: Randomness? = null
    ): Pair<Gene?, Int> {

        /*
            Key -> tableName/columnName
            Value -> raw gene value
         */
        val uniqueColumnValues = mutableMapOf<Pair<String, String>, MutableSet<String>>()

        /*
            Key -> tableName
            Value -> concatenated values of raw genes, in order
         */
        val pksValues = mutableMapOf<String, MutableSet<String>>()

        val allGenes = actions.flatMap { it.seeTopGenes() }

        val sqlActions = mutableListOf<SqlAction>()
        for ((actionIndex, action) in actions.withIndex().sortedBy { it.index }) {

            if (action !is SqlAction) {
                continue
            }
            sqlActions += action

            handleFKs(action, actionIndex)?.let{return it}
            handleUnique(action, actionIndex, uniqueColumnValues, allGenes, randomness)?.let { return it }
            handlePKs(action, actionIndex, pksValues, allGenes, randomness)?.let { return it }
            checkIfTableConstraintsAreSatisfied(action, actionIndex, sqlActions, randomness)?.let { return it }
        }

        // check if all table constraints are satisfied


        //if reached here, then there was no problem
        return Pair(null, -1)
    }

    private fun handleFKs(action: SqlAction, actionIndex: Int): Pair<Gene?, Int>? {

        action.seeTopGenes().flatMap { it.flatView() }
                .filterIsInstance<SqlForeignKeyGene>()
                .firstOrNull { !it.hasValidUniqueIdOfPrimaryKey() }?.let { return Pair(it,actionIndex) }
                ?: return null
    }

    private fun handleUnique(
        action: SqlAction,
        actionIndex: Int,
        uniqueColumnValues: MutableMap<Pair<String, String>, MutableSet<String>>,
        all: List<Gene>,
        randomness: Randomness? = null
    ): Pair<Gene?, Int>? {

        val tableName = action.table.name

        //handle unique constraint
        action.seeTopGenes().forEach { g ->
            val columnName = g.name

            /*
                Is the current gene representing a column in database for which we need
                to enforce a unique constraint?
             */
            val isUnique = action.table.columns.any {
                it.name == columnName && !it.autoIncrement && it.unique
            }

            if (isUnique) {

                val key = Pair(tableName, columnName)

                val existing = uniqueColumnValues.getOrPut(key) { mutableSetOf() }

                val value = getStringValue(g, all)

                if (!existing.contains(value)) {
                    existing.add(value)
                } else {
                    //we have a problem
                    return Pair(g, actionIndex)
                }
            }
        }

        return null
    }

    private fun handlePKs(
        action: SqlAction,
        actionIndex: Int,
        pksValues: MutableMap<String, MutableSet<String>>,
        all: List<Gene>,
        randomness: Randomness? = null
    ): Pair<Gene?, Int>? {

        if (action.table.primaryKeys().isEmpty()) {
            //it can happen that a table has no PK
            return null
        }

        if (action.table.primaryKeys().any { it.autoIncrement }) {
            //auto-increment should never lead to unique violations
            return null
        }

        val tableName = action.table.name

        /*
           Primary Keys are unique as well. However, a PK could be
           defined by several columns, i.e by a tuple.
           So, here we concatenate all PK columns into a single string
           to check for uniqueness
         */

        val pkGenes = action.seeTopGenes()
                .filterIsInstance<SqlPrimaryKeyGene>()

        val pk = pkGenes.sortedBy { it.name }
                .map { it.name + "=" + getStringValue(it, all) }
                .joinToString("__")

        val existing = pksValues.getOrPut(tableName) { mutableSetOf() }

        return if (!existing.contains(pk)) {
            existing.add(pk)
            null
        } else {
            /*
                we have a problem.
                TODO could take one of the offending genes at random?
             */
            Pair(pkGenes.first(), actionIndex)
        }
    }

    private fun getStringValue(g: Gene, all: List<Gene>): String {
        /*
               The code here cannot use Gene#containsSameValueAs, as the same type of
               values could be represented with different gene structures.
               For example, in the case of PKs, those could be regular genes, or
               immutable ones when representing existing data in the DB.
               So, the check for uniqueness is based on value representation...
               but not all values can be printed... in those case we use an ad-hoc
               string with the unique ids.
        */
        return if (g is SqlForeignKeyGene && g.isReferenceToNonPrintable(all)) {
            "FK_REFERENCE_ " + g.uniqueIdOfPrimaryKey
        } else if ((g is SqlPrimaryKeyGene && g.isReferenceToNonPrintable(all))) {
            "FK_REFERENCE_ " + (g.gene as SqlForeignKeyGene).uniqueIdOfPrimaryKey
        } else {
            g.getValueAsPrintableString(all, targetFormat = null)
            /*  TODO: the above code needs to be refactored to get the targetFormat from EMConfig.
                    The target format has an impact on which characters are escaped and may result in compilation errors.
                    The current version performs no escaping of characters by default (i.e. when the target format is null).
            */
        }
    }

    /**
     * repair fk of [sqlAction] based on primary keys of [previous] dbactions
     * @return whether fk is fixed
     */
    fun repairFk(sqlAction: SqlAction, previous: MutableList<SqlAction>) : Pair<Boolean, List<SqlAction>?>{
        val pks = previous.flatMap { it.seeTopGenes() }.filterIsInstance<SqlPrimaryKeyGene>()
        val referSqlActions = mutableListOf<SqlAction>()

        sqlAction.seeTopGenes().flatMap { it.flatView() }.filterIsInstance<SqlForeignKeyGene>().forEach { fk->

            val needToFix = pks.none { p-> p.uniqueId == fk.uniqueIdOfPrimaryKey && p.tableName == fk.targetTable }
            if (needToFix){
                val found = pks.find { fk.targetTable == it.tableName }
                if (found != null){
                    fk.uniqueIdOfPrimaryKey = found.uniqueId
                    referSqlActions.add(previous.find { it.seeTopGenes().contains(found) }!!)
                }else
                    return Pair(false, null)
            }
        }

        return Pair(true, referSqlActions)
    }


    /**
     * @return sorted tables based on its fk
     * @param table to be sorted
     * @param reversed specifies how to sort the tables
     *
     * for instance, table A contains foreign key to table B
     * with inputs setOf(A, B), the return list would be
     *      1) [reversed] is false, listOf(A, B)
     *      2) [reversed] is true, listOf(B, A)
     */
    fun sortTable(table : List<Table>, reversed: Boolean = false) : List<Table>{

        val sorted = table.sortedWith(
            Comparator { o1, o2 ->
                when {
                    o1.foreignKeys.any { t-> t.targetTable.equals(o2.name,ignoreCase = true) } -> -1
                    o2.foreignKeys.any { t-> t.targetTable.equals(o1.name,ignoreCase = true) } -> 1
                    else -> 0
                }
            }
        )
        return if (reversed) sorted.reversed() else sorted
    }

    /**
     * In resource-based individual, SQL actions might be distributed to different set of REST actions regarding resources.
     * In this context, a FK of an insertion may refer to a PK that are in front of this insertion and belongs to other resource (referred resource).
     * During mutation, if the referred resource is modified (e.g., removed), the FK will be broken.
     */
    fun repairFK(sqlAction: SqlAction, previous : MutableList<SqlAction>, createdSqlActions : MutableList<SqlAction>, sqlInsertBuilder: SqlInsertBuilder?, randomness: Randomness) : MutableList<SqlPrimaryKeyGene>{
        val repaired = mutableListOf<SqlPrimaryKeyGene>()
        if(sqlAction.table.foreignKeys.isEmpty())
            return repaired

        val pks = previous.flatMap { it.seeTopGenes() }.filterIsInstance<SqlPrimaryKeyGene>()
        sqlAction.seeTopGenes().flatMap { it.flatView() }.filterIsInstance<SqlForeignKeyGene>().filter { fk-> pks.none { p-> p.uniqueId == fk.uniqueIdOfPrimaryKey } }.forEach { fk->
            var found = pks.find { pk -> pk.tableName == fk.targetTable && pk.uniqueId != fk.uniqueIdOfPrimaryKey }
            if (found == null){
                val created = sqlInsertBuilder?.createSqlInsertionAction(fk.targetTable, mutableSetOf(), enableSingleInsertionForTable= randomness.nextBoolean())?.toMutableList()
                created?:throw IllegalStateException("fail to create insert db action for table (${fk.targetTable})")
                if (log.isTraceEnabled){
                    log.trace("insertion which is created at repairFK is {}",
                        created.joinToString(",") { it.getResolvedName() })
                }
                randomizeDbActionGenes(created, randomness)
                found = created.flatMap { it.seeTopGenes() }.filterIsInstance<SqlPrimaryKeyGene>().find { pk -> pk.tableName == fk.targetTable && pk.uniqueId != fk.uniqueIdOfPrimaryKey }
                    ?:throw IllegalStateException("fail to create target table (${fk.targetTable}) for ${fk.name}")

                repairFkForInsertions(created)
                createdSqlActions.addAll(created)
                previous.addAll(created)
                repaired.addAll(created.flatMap { it.seeTopGenes() }.filterIsInstance<SqlPrimaryKeyGene>())
            }
            fk.uniqueIdOfPrimaryKey = found.uniqueId
            repaired.add(found)
        }

        return repaired
    }

    fun repairFkForInsertions(sqlActions: List<SqlAction>){
        sqlActions.forEachIndexed { index, dbAction ->
            val fks = dbAction.seeTopGenes().flatMap { it.flatView() }.filterIsInstance<SqlForeignKeyGene>()
            if (fks.any { !it.nullable && !it.isBound() } && index == 0)
                throw IllegalStateException("invalid insertion, there exists invalid fk at $index")
            val pks = sqlActions.subList(0, index).flatMap { it.seeTopGenes() }.filterIsInstance<SqlPrimaryKeyGene>()
            fks.filter { !it.nullable && !it.isBound() || pks.none { p->p.uniqueId == it.uniqueIdOfPrimaryKey }}.forEach {fk->
                val found = pks.find { pk -> pk.tableName.equals(fk.targetTable, ignoreCase = true) }
                    ?: throw IllegalStateException("fail to target table ${fk.targetTable} for the fk ${fk.name}")
                fk.uniqueIdOfPrimaryKey = found.uniqueId
            }
        }
        if (!verifyForeignKeys(sqlActions))
            throw IllegalStateException("FK repair fails")
    }

    /**
     * @return a list of dbactions from [sqlActions] whose related table is [tableName]
     */
    fun findDbActionsByTableName(sqlActions: List<SqlAction>, tableName : String) : List<SqlAction>{
        return sqlActions.filter { it.table.name.equals(tableName, ignoreCase = true) }
    }

}