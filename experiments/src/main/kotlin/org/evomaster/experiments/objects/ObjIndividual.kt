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
                    var usedObj : MutableMap<Pair<String, String> , ObjectGene> = mutableMapOf(),
                    val dbInitialization: MutableList<DbAction> = mutableListOf()
                    //var usedObj : MutableList<ObjectGene> = mutableListOf()
                    //var usedObj : MutableMap<Pair<ObjRestCallAction, String> , ObjectGene> = mutableMapOf()

) : Individual() {

    override fun copy(): Individual {
        return ObjIndividual(
                callActions.map { a -> a.copy() as RestAction } as MutableList<RestAction>,
                sampleType,
                usedObj.filterKeys { it -> callActions.any{a -> a.toString().contains(it.first)} }.toMutableMap(),
                dbInitialization.map { d -> d.copy() as DbAction } as MutableList<DbAction>
                //usedObj.map { (k, u) ->
                //    usedObj[Pair((k.first.copy() as ObjRestCallAction), (k.second.copy() as Gene))] = u.copy() as ObjectGene} as MutableMap<Pair<ObjRestCallAction, Gene> , ObjectGene>
                //usedObj.map { u -> u.copy() as ObjectGene} as MutableMap<Pair<ObjRestCallAction, Gene> , ObjectGene>
                //usedObj = mutableMapOf()

        )
    }

    override fun canMutateStructure(): Boolean {
        return sampleType == SampleType.RANDOM ||
                sampleType == SampleType.SMART_GET_COLLECTION
    }


    override fun seeGenes(filter: GeneFilter): List<out Gene> {

        return when(filter){
            /*GeneFilter.ALL ->  dbInitialization.flatMap(DbAction::seeGenes)
                    .plus(callActions.flatMap(RestAction::seeGenes))*/
            GeneFilter.ALL ->  dbInitialization.flatMap(DbAction::seeGenes)
                    .plus(usedObj.values.flatMap(ObjectGene::flatView))


            //GeneFilter.NO_SQL -> callActions.flatMap(RestAction::seeGenes)
            GeneFilter.NO_SQL -> usedObj.values.flatMap(ObjectGene::flatView)
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

        //TODO: is this a place for the coherence check between usedObj and actions?

    }

    override fun seeInitializingActions() : List<Action>{
        return dbInitialization
    }

    fun coherenceCheck() : Boolean{


        callActions.forEach{ action ->
            action.seeGenes().forEach{ gene ->
                usedObj.get(Pair(action.toString(), gene.getVariableName()))
                //the object has been mutated (?) so the next step is to update the action/gene combination to match
                // next step: pick which of the object's optional genes to use? (substring?)
                // same as selectedGene from the ObjRestSampler. Might make sense to just store that as well?
            }
        }

        /*
        TODO: Refactor (BMR)
        the usedObj becomes a separate class/object, attached to each individual
        it stores and returns   - the objects used (ensuring coherence and non-repetition)
                                - links between objects and actions/genes and objects/genes used
                                - ensures coherence (propagate object mutations forward and action results backward)
         */


        return true
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
            //rez += r.getName() + "\n"
            rez += r.toString() + "\n"
        }
        return rez
    }
}