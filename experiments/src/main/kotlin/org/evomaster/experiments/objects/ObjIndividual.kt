package org.evomaster.experiments.objects

import org.evomaster.core.database.DbAction
import org.evomaster.core.search.Action
import org.evomaster.core.search.Individual
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.GeneUtils
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.problem.rest.RestAction
import org.evomaster.core.problem.rest.SampleType


class ObjIndividual(val callActions: MutableList<RestAction>,
                    val sampleType: SampleType,
                    var uo: UsedObj,
                    val dbInitialization: MutableList<DbAction> = mutableListOf()

) : Individual() {

    override fun copy(): Individual {
        return ObjIndividual(
                callActions.map { a -> a.copy() as RestAction } as MutableList<RestAction>,
                sampleType,
                uo.copy(),
                dbInitialization.map { d -> d.copy() as DbAction } as MutableList<DbAction>

        )
    }

    override fun canMutateStructure(): Boolean {
        return sampleType == SampleType.RANDOM ||
                sampleType == SampleType.SMART_GET_COLLECTION
    }


    override fun seeGenes(filter: GeneFilter): List<out Gene> {

        return when(filter){
            GeneFilter.ALL ->  dbInitialization.flatMap(DbAction::seeGenes)
                    .plus(uo.usedObjects())

            GeneFilter.NO_SQL -> uo.usedObjects()
            GeneFilter.ONLY_SQL -> dbInitialization.flatMap(DbAction::seeGenes)
        }
    }

    /*
        TODO Tricky... should dbInitialization somehow be part of the size?
        But they are merged in a single operation in a single call...
        need to think about it
     */

    override fun size() = callActions.size

    override fun seeActions(): List<out Action> {
        return callActions
    }

    override fun verifyInitializationActions(): Boolean {
        return DbAction.verifyActions(dbInitialization.filterIsInstance<DbAction>())
    }

    override fun repairInitializationActions(randomness: Randomness) {

        /**
         * First repair SQL Genes (i.e. SQL Timestamps)
         */
        GeneUtils.repairGenes(this.seeGenes(Individual.GeneFilter.ONLY_SQL).flatMap { it.flatView() })

        /**
         * Now repair databse constraints (primary keys, foreign keys, unique fields, etc.)
         */
        if (!verifyInitializationActions()) {
            DbAction.repairBrokenDbActionsList(dbInitialization, randomness)
            assert(verifyInitializationActions())
        }

    }

    override fun seeInitializingActions() : List<Action>{
        return dbInitialization
    }

    fun debugginPrint() : String{
        var rez = ""
        for(r in this.callActions){
            rez += r.getName() + "\n"
            //rez += r.seeGenes() + "\n"
        }
        return rez
    }
    fun debugginPrintProcessed() : String{
        var rez = ""
        for(r in this.callActions){
            rez += r.toString() + "\n"
        }
        return rez
    }
}