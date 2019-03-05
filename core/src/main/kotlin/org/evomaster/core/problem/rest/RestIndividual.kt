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
                     val usedObjects: UsedObjs = UsedObjs()
) : Individual() {

    override fun copy(): Individual {
        return RestIndividual(
                actions.map { a -> a.copy() as RestAction } as MutableList<RestAction>,
                sampleType,
                dbInitialization.map { d -> d.copy() as DbAction } as MutableList<DbAction>,
                usedObjects.copy()
        )
    }

    override fun canMutateStructure(): Boolean {
        return sampleType == SampleType.RANDOM ||
                sampleType == SampleType.SMART_GET_COLLECTION
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
                    val innerGene = when (gene::class) {
                        OptionalGene::class -> (gene as OptionalGene).gene
                        DisruptiveGene::class -> (gene as DisruptiveGene<*>).gene
                        else -> gene
                    }
                    val relevantGene = usedObjects.getRelevantGene((action as RestCallAction), innerGene)
                    when (action::class) {
                        RestCallAction::class -> {
                            when (relevantGene::class) {
                                OptionalGene::class -> (relevantGene as OptionalGene).gene.copyValueFrom(innerGene)
                                DisruptiveGene::class -> (relevantGene as DisruptiveGene<*>).gene.copyValueFrom(innerGene)
                                ObjectGene::class -> relevantGene.copyValueFrom(innerGene)
                                else -> relevantGene.copyValueFrom(innerGene)
                            }
                        }
                    }
                }
                catch (e: Exception){
                    // TODO BMR: EnumGene is not handled well and ends up here.
                     return false
                }
            }
        }
        return true
    }
}