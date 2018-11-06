package org.evomaster.core.problem.rest

import org.evomaster.core.database.DbAction
import org.evomaster.core.search.Action
import org.evomaster.core.search.Individual
import org.evomaster.core.search.gene.Gene


class ObjIndividual(val actions: MutableList<RestAction>,
                     val sampleType: SampleType,
                     val dbInitialization: MutableList<DbAction> = mutableListOf()
) : Individual() {

    override fun copy(): Individual {
        return RestIndividual(
                actions.map { a -> a.copy() as RestAction } as MutableList<RestAction>,
                sampleType,
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
                    .plus(actions.flatMap(RestAction::seeGenes))

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

    override fun seeInitializingActions() : List<Action>{
        return dbInitialization
    }

    fun debugginPrint() : String{
        var rez = ""
        for(r in this.actions){
            rez += r.getName() + "\n"
            //rez += r.seeGenes() + "\n"
        }
        return rez
    }
    fun debugginPrintProcessed() : String{
        var rez = ""
        for(r in this.actions){
            //rez += r.getName() + "\n"
            rez += r.toString() + "\n"
        }
        return rez
    }
}