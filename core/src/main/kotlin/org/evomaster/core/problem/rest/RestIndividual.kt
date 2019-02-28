package org.evomaster.core.problem.rest

import org.evomaster.core.Lazy
import org.evomaster.core.database.DbAction
import org.evomaster.core.database.DbActionUtils
import org.evomaster.core.search.Action
import org.evomaster.core.search.Individual
import org.evomaster.core.search.gene.*
import org.evomaster.core.search.service.Randomness


class RestIndividual(val actions: MutableList<RestAction>,
                     val sampleType: SampleType,
                     val dbInitialization: MutableList<DbAction> = mutableListOf(),
                     val usedObject: UsedObjs = UsedObjs()
) : Individual() {

    override fun copy(): Individual {
        return RestIndividual(
                actions.map { a -> a.copy() as RestAction } as MutableList<RestAction>,
                sampleType,
                dbInitialization.map { d -> d.copy() as DbAction } as MutableList<DbAction>,
                usedObject.copy()
        )
    }

    override fun canMutateStructure(): Boolean {
        return sampleType == SampleType.RANDOM ||
                sampleType == SampleType.SMART_GET_COLLECTION
    }


    fun seeGenes2(filter: GeneFilter): List<out Gene> {

        return when (filter) {
            GeneFilter.ALL -> {
                if(usedObject.isEmpty()) dbInitialization.flatMap(DbAction::seeGenes)
                        .plus(actions.flatMap(RestAction::seeGenes))
                else
                    dbInitialization.flatMap(DbAction::seeGenes)
                            .plus(usedObject.usedObjects())
            }

            GeneFilter.NO_SQL -> {
                if(usedObject.isEmpty()) actions.flatMap(RestAction::seeGenes)
                else usedObject.usedObjects()
            }
            GeneFilter.ONLY_SQL -> dbInitialization.flatMap(DbAction::seeGenes)

        }
    }

    override fun seeGenes(filter: GeneFilter): List<out Gene> {

        return when (filter) {
            GeneFilter.ALL -> dbInitialization.flatMap(DbAction::seeGenes).plus(actions.flatMap(RestAction::seeGenes))
            GeneFilter.NO_SQL -> actions.flatMap(RestAction::seeGenes)
            GeneFilter.ONLY_SQL -> dbInitialization.flatMap(DbAction::seeGenes)

        }
    }

    /*
        TODO Tricky... should dbInitialization somehow be part of the size?
        But they are merged in a single operation in a single call...
        need to think about it
     */

    override fun size() = actions.size

    override fun seeActions(): List<out Action> {
        return actions
    }

    override fun seeInitializingActions(): List<Action> {
        return dbInitialization
    }

    override fun verifyInitializationActions(): Boolean {
        enforceCoherence()
        return DbActionUtils.verifyActions(dbInitialization.filterIsInstance<DbAction>())
    }


    override fun repairInitializationActions(randomness: Randomness) {

        /**
         * First repair SQL Genes (i.e. SQL Timestamps)
         */
        GeneUtils.repairGenes(this.seeGenes(Individual.GeneFilter.ONLY_SQL).flatMap { it.flatView() })

        /**
         * Now repair database constraints (primary keys, foreign keys, unique fields, etc.)
         */
        if (!verifyInitializationActions()) {
            DbActionUtils.repairBrokenDbActionsList(dbInitialization, randomness)
            Lazy.assert{verifyInitializationActions()}
        }
    }

    fun enforceCoherence(): Boolean {
        actions.forEach { action ->
            action.seeGenes().forEach { gene ->
                try {
                    val relevantGene = usedObject.getRelevantGene((action as RestCallAction), gene)
                    when (action::class) {
                        RestCallAction::class -> {
                            when (gene::class) {
                                OptionalGene::class -> (relevantGene as OptionalGene).gene.copyValueFrom((gene as OptionalGene).gene)
                                DisruptiveGene::class -> (relevantGene as OptionalGene).gene.copyValueFrom((gene as DisruptiveGene<*>).gene)
                                ObjectGene::class -> relevantGene.copyValueFrom(gene)
                                else -> relevantGene.copyValueFrom(gene)
                            }
                        }
                    }
                }
                catch (e: Exception){
                    return false
                }
            }
        }
        return true
    }

    fun enforceCoherence2(): Boolean{
        actions.forEach { action ->
            action.seeGenes().forEach { gene ->
                //TODO: simplify this
                try {
                    val relevantGene = usedObject.getRelevantGene((action as RestCallAction), gene)
                    when (action::class) {
                        RestCallAction::class -> {
                            when (gene::class) {
                                OptionalGene::class -> (gene as OptionalGene).gene.copyValueFrom((relevantGene as OptionalGene).gene)
                                DisruptiveGene::class -> (gene as DisruptiveGene<*>).gene.copyValueFrom((relevantGene as OptionalGene).gene)
                                ObjectGene::class -> gene.copyValueFrom(relevantGene)
                                else -> gene.copyValueFrom(relevantGene)
                            }

                        }
                    }
                }
                catch(e: Exception) {
                    return false
                }

            }
        }
        return true
    }
}