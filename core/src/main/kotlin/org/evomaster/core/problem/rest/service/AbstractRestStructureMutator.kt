package org.evomaster.core.problem.rest.service

import org.evomaster.core.problem.rest.RestIndividual
import org.evomaster.core.search.Action
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.service.mutator.MutatedGeneSpecification
import org.evomaster.core.search.service.mutator.StructureMutator
import org.slf4j.Logger
import org.slf4j.LoggerFactory

abstract class AbstractRestStructureMutator : StructureMutator(){

    companion object {
        private val log: Logger = LoggerFactory.getLogger(AbstractRestStructureMutator::class.java)
    }
    abstract fun getSampler() : AbstractRestSampler


    override fun addInitializingActions(individual: EvaluatedIndividual<*>, mutatedGenes: MutatedGeneSpecification?) {

        if (!config.shouldGenerateSqlData()) {
            return
        }

        val ind = individual.individual as? RestIndividual
            ?: throw IllegalArgumentException("Invalid individual type")

        val fw = individual.fitness.getViewOfAggregatedFailedWhere()
            //TODO likely to remove/change once we ll support VIEWs
            .filter { getSampler().canInsertInto(it.key) }

        if (fw.isEmpty()) {
            return
        }

        val old = mutableListOf<Action>().plus(ind.seeInitializingActions())

        val addedInsertions = handleFailedWhereSQL(ind, fw, mutatedGenes, getSampler())

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

    fun handleFailedWhereSQL(
        ind: RestIndividual, fw: Map<String, Set<String>>,
        mutatedGenes: MutatedGeneSpecification?, sampler: AbstractRestSampler): MutableList<List<Action>>?{

        /*
            because there might exist representExistingData in db actions which are in between rest actions,
            we use seeDbActions() instead of seeInitializingActions() here

            Man: shall we add all existing data here?
         */
        if(ind.seeDbAction().isEmpty()
            || ! ind.seeDbAction().any { it.representExistingData }) {
            //add existing data only once
            ind.dbInitialization.addAll(0, sampler.existingSqlData)

            //record newly added existing sql data
            mutatedGenes?.addedExistingDataInitialization?.addAll(0, sampler.existingSqlData)

            if (log.isTraceEnabled)
                log.trace("{} existingSqlData are added", sampler.existingSqlData)
        }

        val max = config.maxSqlInitActionsPerMissingData

        var missing = findMissing(fw, ind)

        val addedInsertions = if (mutatedGenes != null) mutableListOf<List<Action>>() else null

        while (!missing.isEmpty()) {

            val first = missing.entries.first()

            val k = randomness.nextInt(1, max)

            (0 until k).forEach {
                val insertions = sampler.sampleSqlInsertion(first.key, first.value)
                /*
                    New action should be before existing one, but still after the
                    initializing ones
                 */
//                val position = sampler.existingSqlData.size
                val position = ind.dbInitialization.indexOfLast { it.representExistingData } + 1
                ind.dbInitialization.addAll(position, insertions)

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
            missing = findMissing(fw, ind)
        }

        if (config.generateSqlDataWithDSE) {
            //TODO DSE could be plugged in here
        }

        return addedInsertions
    }

    private fun findMissing(fw: Map<String, Set<String>>, ind: RestIndividual): Map<String, Set<String>> {

        return fw.filter { e ->
            //shouldn't have already an action adding such SQL data
            ind.seeInitializingActions()
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

}