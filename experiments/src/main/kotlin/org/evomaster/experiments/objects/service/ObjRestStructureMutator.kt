package org.evomaster.experiments.objects.service

import com.google.inject.Inject
import org.evomaster.core.Lazy
import org.evomaster.core.database.DatabaseExecution
import org.evomaster.core.problem.rest.HttpVerb
import org.evomaster.core.problem.rest.SampleType
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Individual
import org.evomaster.core.search.service.mutator.MutatedGeneSpecification
import org.evomaster.core.search.service.mutator.StructureMutator
import org.evomaster.experiments.objects.ObjIndividual
import org.evomaster.experiments.objects.ObjRestCallAction



class ObjRestStructureMutator : StructureMutator() {

    @Inject
    private lateinit var sampler: ObjRestSampler

    override fun addInitializingActions(individual: EvaluatedIndividual<*>, mutatedGene: MutatedGeneSpecification?) {

        if (!config.shouldGenerateSqlData()) {
            return
        }

        val ind = individual.individual as? ObjIndividual
                ?: throw IllegalArgumentException("Invalid individual type")

//        val es = individual.fitness.emptySelects
//                ?: return
//
//        if (es.queriedData.isEmpty()) {
//            return
//        }
//
//        val max = config.maxSqlInitActionsPerMissingData
//
//        var missing = findMissing(es, ind)
//
//        while (!missing.isEmpty()) {
//
//            val first = missing.entries.first()
//
//            val k = randomness.nextInt(1, max)
//
//            (0 until k).forEach {
//                val insertions = sampler.sampleSqlInsertion(first.key, first.value)
//                ind.dbInitialization.addAll(0, insertions)
//            }
//
//            /*
//                When we miss A and B, and we add for A, it can still happen that
//                then B is covered as well. For example, if A has a non-null
//                foreign key to B, then generating an action for A would also
//                imply generating an action for B as well.
//                So, we need to recompute "missing" each time
//             */
//            missing = findMissing(es, ind)
//        }
//
//        ind.dbInitialization.addAll(0, sampler.existingSqlData)
//
//        if (config.generateSqlDataWithDSE) {
//            //TODO DSE could be plugged in here
//        }
//
//        ind.repairInitializationActions(randomness)

    }

    private fun findMissing(es: DatabaseExecution, ind: ObjIndividual): Map<String, Set<String>> {

        return es.queriedData.filter { e ->
            //shouldn't have already an action adding such SQL data
            ind.dbInitialization.none { a ->
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


    override fun mutateStructure(individual: Individual, mutatedGene: MutatedGeneSpecification?) {
        if (individual !is ObjIndividual) {
            throw IllegalArgumentException("Invalid individual type")
        }

        /*
            TODO should be enable the adding/removing of SQL commands here?
            Or should that be better to handle with DSE?
         */


        if (!individual.canMutateStructure()) {
            return // nothing to do
        }

        when (individual.sampleType) {
            SampleType.RANDOM -> mutateForRandomType(individual, mutatedGene)

            SampleType.SMART_GET_COLLECTION -> mutateForSmartGetCollection(individual, mutatedGene)

            SampleType.SMART -> throw IllegalStateException(
                    "SMART sampled individuals shouldn't be marked for structure mutations")

            //this would be a bug
            else -> throw IllegalStateException("Cannot handle sample type ${individual.sampleType}")
        }
    }

    private fun mutateForSmartGetCollection(ind: ObjIndividual, mutatedGene: MutatedGeneSpecification?) {
        /*
            recall: in this case, we have 1 or more POST on same
            collection, followed by a single GET

            However, in case of path parameters (eg "/x/{id}/collection")
            before the collection endpoint, there might be one or more POSTs
            to setup the intermediary resources
         */

        (0 until ind.actions.size - 1).forEach {
            val a = ind.actions[it]
            Lazy.assert{a !is ObjRestCallAction || a.verb == HttpVerb.POST}
        }
        Lazy.assert{ val a = ind.actions.last(); a is ObjRestCallAction && a.verb == HttpVerb.GET }

        val indices = ind.actions.indices
                .filter { i ->
                    val a = ind.actions[i]
                    /*
                        one simple way to distinguish the POST on collection is that
                        they are not chaining a location, as GET is on same endpoint
                    */
                    a is ObjRestCallAction && !a.saveLocation && a.verb == HttpVerb.POST
                }

        if (indices.isEmpty()) {
            /*
                Nothing we can do here. Cannot delete a POST, and
                neither add a new one, as we have no template for
                it in the test to duplicate.
             */
            return
        }

        if (indices.size > 1 &&
                (randomness.nextBoolean() ||
                        ind.actions.size == config.maxTestSize)) {

            //delete one POST, but NOT the GET
            val chosen = randomness.choose(indices)
            ind.actions.removeAt(chosen)

        } else {
            //insert a new POST on the collection
            val idx = indices.last()

            val postTemplate = ind.actions[idx] as ObjRestCallAction
            Lazy.assert{postTemplate.verb == HttpVerb.POST && !postTemplate.saveLocation}

            val post = sampler.createActionFor(postTemplate, ind.actions.last() as ObjRestCallAction)

            /*
                where it is inserted should not matter, as long as
                it is before the last GET, but after all the other initializing
                POSTs
             */
            ind.actions.add(idx, post)
        }
    }

    private fun mutateForRandomType(ind: ObjIndividual, mutatedGene: MutatedGeneSpecification?) {

        if (ind.actions.size == 1) {
            val action = sampler.sampleRandomAction(0.05)

            //save mutated genes
            mutatedGene?.addedGenes?.addAll(action.seeGenes())
            mutatedGene?.mutatedPosition?.add(ind.size())

            ind.actions.add(action)
            return
        }

        if (randomness.nextBoolean() || ind.actions.size == config.maxTestSize) {

            //delete one at random
            val chosen = randomness.nextInt(ind.actions.size)

            //save mutated genes
            mutatedGene?.removedGene?.addAll(ind.seeActions()[chosen].seeGenes())
            mutatedGene?.mutatedPosition?.add(chosen)

            ind.actions.removeAt(chosen)

        } else {

            //add one at random
            val action = sampler.sampleRandomAction(0.05)
            val chosen = randomness.nextInt(ind.actions.size)

            //save mutated genes
            mutatedGene?.addedGenes?.addAll(action.seeGenes())
            mutatedGene?.mutatedPosition?.add(chosen)

            ind.actions.add(chosen, action)
        }

    }
}