package org.evomaster.core.problem.api.service

import org.evomaster.core.Lazy
import org.evomaster.core.database.DbAction
import org.evomaster.core.database.DbActionUtils
import org.evomaster.core.database.SqlInsertBuilder
import org.evomaster.core.search.Action
import org.evomaster.core.search.Individual
import org.evomaster.core.search.gene.sql.SqlForeignKeyGene
import org.evomaster.core.search.gene.sql.SqlPrimaryKeyGene
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.service.mutator.MutatedGeneSpecification
import org.evomaster.core.search.service.mutator.StructureMutator
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.math.max
import kotlin.math.min

/**
 * the abstract structure mutator for API based SUT, such as REST, GraphQL, RPC
 */
abstract class ApiWsStructureMutator : StructureMutator(){

    companion object {
        private val log: Logger = LoggerFactory.getLogger(ApiWsStructureMutator::class.java)
    }

    fun<T : ApiWsIndividual> addInitializingActions(individual: EvaluatedIndividual<*>, mutatedGenes: MutatedGeneSpecification?, sampler: ApiWsSampler<T>) {
        if (!config.shouldGenerateSqlData()) {
            return
        }

        val ind = individual.individual as? T
                ?: throw IllegalArgumentException("Invalid individual type")

        val fw = individual.fitness.getViewOfAggregatedFailedWhere()
                //TODO likely to remove/change once we ll support VIEWs
                .filter { sampler.canInsertInto(it.key) }

        if (fw.isEmpty()) {
            return
        }

        val old = mutableListOf<Action>().plus(ind.seeInitializingActions())

        val addedInsertions = handleFailedWhereSQL(ind, fw, mutatedGenes, sampler)

        ind.repairInitializationActions(randomness)
        // update impact based on added genes
        if(mutatedGenes != null && config.isEnabledArchiveGeneSelection()){
            individual.updateImpactGeneDueToAddedInitializationGenes(
                    mutatedGenes,
                    old,
                    addedInsertions
            )
        }
    }

    fun<T : ApiWsIndividual> handleFailedWhereSQL(
        ind: T, fw: Map<String, Set<String>>,
        mutatedGenes: MutatedGeneSpecification?, sampler: ApiWsSampler<T>
    ): MutableList<List<Action>>?{

        /*
            because there might exist representExistingData in db actions which are in between rest actions,
            we use seeDbActions() instead of seeInitializingActions() here

            TODO
            Man: with config.maximumExistingDataToSampleInD,
                we might remove the condition check on representExistingData.
         */
        if(ind.seeDbActions().isEmpty()
            || ! ind.seeDbActions().any { it is DbAction && it.representExistingData }) {

            /*
                tmp solution to set maximum size of executing existing data in sql
             */
            val existing = if (config.maximumExistingDataToSampleInDb > 0 && sampler.existingSqlData.size > config.maximumExistingDataToSampleInDb)
                        randomness.choose(sampler.existingSqlData, config.maximumExistingDataToSampleInDb)
                    else sampler.existingSqlData

            //add existing data only once
            ind.addInitializingActions(0, existing)

            //record newly added existing sql data
            mutatedGenes?.addedExistingDataInitialization?.addAll(0, existing)

            if (log.isTraceEnabled)
                log.trace("{} existingSqlData are added", existing)
        }

        // add fw into dbInitialization
        val max = config.maxSqlInitActionsPerMissingData
        val initializingActions = ind.seeInitializingActions()

        var missing = findMissing(fw, initializingActions)

        val addedInsertions = if (mutatedGenes != null) mutableListOf<List<Action>>() else null

        while (!missing.isEmpty()) {

            val first = missing.entries.first()

            val k = randomness.nextInt(1, max)

            (0 until k).forEach { _ ->
                val insertions = sampler.sampleSqlInsertion(first.key, first.value)
                /*
                    New action should be before existing one, but still after the
                    initializing ones
                 */
//                val position = sampler.existingSqlData.size
                val position = ind.seeInitializingActions().indexOfLast { it is DbAction && it.representExistingData } + 1
                ind.addInitializingActions(position, insertions)

                if (log.isTraceEnabled)
                    log.trace("{} insertions are added", insertions.size)

                //record newly added insertions
                addedInsertions?.add(0, insertions)
            }

            /*
                When we miss A and B, and we add for A, it can still happen that
                then B is covered as well. For example, if A has a non-null
                foreign key to B, then generating an action for A would also
                imply generating an action for B as well.
                So, we need to recompute "missing" each time
             */
            missing = findMissing(fw, ind.seeInitializingActions())
        }

        if (config.generateSqlDataWithDSE) {
            //TODO DSE could be plugged in here
        }

        return addedInsertions
    }

    private fun findMissing(fw: Map<String, Set<String>>, dbactions: List<DbAction>): Map<String, Set<String>> {

        return fw.filter { e ->
            //shouldn't have already an action adding such SQL data
            dbactions
                .filter { ! it.representExistingData }
                .none { a ->
                    a.table.name.equals(e.key, ignoreCase = true) && e.value.all { c ->
                        // either the selected column is already in existing action
                        (c != "*" && a.selectedColumns.any { x ->
                            x.name.equals(c, ignoreCase = true)
                        }) // or we want all, and existing action has all columns
                                || (c == "*" && a.table.columns.map { it.name.toLowerCase() }
                            .containsAll(a.selectedColumns.map { it.name.toLowerCase() }))
                    }
                }
        }
    }

    override fun mutateInitStructure(individual: Individual, evaluatedIndividual: EvaluatedIndividual<*>, mutatedGenes: MutatedGeneSpecification?, targets: Set<Int>) {
        Lazy.assert { individual is ApiWsIndividual }
        individual as ApiWsIndividual

        /*
           modify one table at once, and add/remove [n] rows for it. however, never totally remove the table.
           note that if there is no any init sql, we randomly select one table to add.
        */

        val candidatesToMutate = individual.seeInitializingActions().filterNot { it.representExistingData }
        val tables = candidatesToMutate.map { it.table.name }.run {
            ifEmpty { getSqlInsertBuilder()!!.getTableNames() }
        }

        val table = randomness.choose(tables)
        val total = tables.count { it == table }

        if (total == 1 || randomness.nextBoolean()){
            // add action
            val num = randomness.nextInt(1, max(1, getMaxSizeOfMutatingInitAction()))
            val add = createInsertSqlAction(table, num)
            handleInitSqlAddition(individual, add, mutatedGenes)

        }else{
            // remove action
            val num = randomness.nextInt(1, max(1, min(total-1, getMaxSizeOfMutatingInitAction())))
            val candidates = candidatesToMutate.filter { it.table.name == table }
            val remove = randomness.choose(candidates, num)

            handleInitSqlRemoval(individual, remove, mutatedGenes)
        }
    }

    /**
     * add specified actions (i.e., [add]) into initialization of [individual]
     */
    fun handleInitSqlAddition(individual: ApiWsIndividual, add: List<List<DbAction>>, mutatedGenes: MutatedGeneSpecification?){
        individual.addInitializingActions(actions = add.flatten())
        mutatedGenes?.addedDbActions?.addAll(add)
    }

    /**
     * remove specified actions (i.e., [remove]) from initialization of [individual]
     */
    fun handleInitSqlRemoval(individual: ApiWsIndividual, remove: List<DbAction>, mutatedGenes: MutatedGeneSpecification?){
        val relatedRemove = mutableListOf<DbAction>()
        relatedRemove.addAll(remove)
        remove.forEach {
            getRelatedRemoveDbActions(individual, it, relatedRemove)
        }
        val set = relatedRemove.filterNot { it.representExistingData }.toSet().toMutableList()
        mutatedGenes?.removedDbActions?.addAll(set.map { it to individual.seeInitializingActions().indexOf(it) })
        individual.removeInitDbActions(set)
    }

    private fun getRelatedRemoveDbActions(ind: ApiWsIndividual, remove : DbAction, relatedRemove: MutableList<DbAction>){
        val pks = remove.seeGenes().flatMap { it.flatView() }.filterIsInstance<SqlPrimaryKeyGene>()
        val index = ind.seeInitializingActions().indexOf(remove)
        if (index < ind.seeInitializingActions().size - 1 && pks.isNotEmpty()){
            val removeDbFKs = ind.seeInitializingActions().subList(index + 1, ind.seeInitializingActions().size).filter {
                it.seeGenes().flatMap { g-> g.flatView() }.filterIsInstance<SqlForeignKeyGene>()
                        .any {fk-> pks.any {pk->fk.uniqueIdOfPrimaryKey == pk.uniqueId} } }
            relatedRemove.addAll(removeDbFKs)
            removeDbFKs.forEach {
                getRelatedRemoveDbActions(ind, it, relatedRemove)
            }
        }
    }

    /**
     * @param name is the table name
     * @param num is a number of table with [name] to be added
     */
    fun createInsertSqlAction(name : String, num : Int) : List<List<DbAction>>{
        getSqlInsertBuilder() ?:throw IllegalStateException("attempt to create resource with SQL but the sqlBuilder is null")
        if (num <= 0)
            throw IllegalArgumentException("invalid num (i.e.,$num) for creating resource")

        val list= (0 until num).map { getSqlInsertBuilder()!!.createSqlInsertionAction(name, setOf()) }.toMutableList()

        if (log.isTraceEnabled){
            log.trace("at createDbActions, {} insertions are added, and they are {}", list.size,
                    list.flatten().joinToString(",") {
                        it.getResolvedName()
                    })
        }

        DbActionUtils.randomizeDbActionGenes(list.flatten(), randomness)
        DbActionUtils.repairBrokenDbActionsList(list.flatten().toMutableList(), randomness)
        return list
    }

    abstract fun getSqlInsertBuilder() : SqlInsertBuilder?

    override fun canApplyInitStructureMutator(): Boolean {
        return (config.initStructureMutationProbability > 0 && config.maxSizeOfMutatingInitAction > 0) && getSqlInsertBuilder() != null
    }
}