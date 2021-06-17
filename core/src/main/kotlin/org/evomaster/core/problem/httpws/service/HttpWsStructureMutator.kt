package org.evomaster.core.problem.httpws.service

import org.evomaster.core.database.DbAction
import org.evomaster.core.search.Action
import org.evomaster.core.search.service.mutator.MutatedGeneSpecification
import org.evomaster.core.search.service.mutator.StructureMutator
import org.slf4j.Logger
import org.slf4j.LoggerFactory

abstract class HttpWsStructureMutator : StructureMutator(){

    companion object {
        private val log: Logger = LoggerFactory.getLogger(HttpWsStructureMutator::class.java)
    }


    fun<T : HttpWsIndividual> handleFailedWhereSQL(
        ind: T, fw: Map<String, Set<String>>,
        mutatedGenes: MutatedGeneSpecification?, sampler: HttpWsSampler<T>
    ): MutableList<List<Action>>?{

        /*
            because there might exist representExistingData in db actions which are in between rest actions,
            we use seeDbActions() instead of seeInitializingActions() here
         */
        if(ind.seeDbActions().isEmpty()
            || ! ind.seeDbActions().any { it is DbAction && it.representExistingData }) {

            //add existing data only once
            ind.addInitializingActions(0, sampler.existingSqlData)

            //record newly added existing sql data
            mutatedGenes?.addedExistingDataInitialization?.addAll(0, sampler.existingSqlData)

            if (log.isTraceEnabled)
                log.trace("{} existingSqlData are added", sampler.existingSqlData)
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
}