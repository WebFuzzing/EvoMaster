package org.evomaster.core.problem.rest.serviceII

import org.evomaster.core.database.DbAction
import org.evomaster.core.problem.rest.RestIndividual
import org.evomaster.core.problem.rest.SampleType
import org.evomaster.core.problem.rest.serviceII.resources.RestResourceCalls
import org.evomaster.core.search.Action
import org.evomaster.core.search.Individual
import java.lang.IllegalStateException

class RestIndividualII(val resourceCalls: MutableList<RestResourceCalls>,
                       sampleType: SampleType,
                       dbInitialization: MutableList<DbAction> = mutableListOf()
) : RestIndividual(resourceCalls.flatMap { it.actions }.toMutableList(), sampleType, dbInitialization) {

    override fun copy(): Individual {
        val calls = resourceCalls.map { it.copy() }.toMutableList()
        return RestIndividualII(
                calls,
                sampleType,
                dbInitialization.map { d -> d.copy() as DbAction } as MutableList<DbAction>
        )
    }

    override fun seeActions(): List<out Action> {
        if(resourceCalls.map { it.actions.size }.sum() != actions.size)
            throw  IllegalStateException("Mismatched between RestResourceCalls and actions")
        return actions
    }

    fun removeActionsFrom(last : Int) : Boolean{
        //update
        if(last != actions.size -1){
            //remove resourceCalls
            var loc = 0
            var rloc = 0
            resourceCalls.forEachIndexed { index, rrCalls ->
                loc += rrCalls.actions.size
                if(rloc == 0 && loc - 1 >= last){
                    rloc = index
                    loc = rrCalls.actions.size - (loc - 1 - last)
                    while(rrCalls.actions.size != loc){
                        rrCalls.actions.removeAt(rrCalls.actions.size - 1)
                    }
                }
            }

            if(rloc != resourceCalls.size -1){
                while (resourceCalls.size != rloc+1)
                    resourceCalls.removeAt(this.resourceCalls.size - 1)
            }

            while(actions.size != last + 1){
                actions.removeAt(actions.size - 1)
            }
        }
        return actions.size == resourceCalls.map { it.actions.size }.sum()
    }

    fun updateActionCluster(){
        resourceCalls.forEach {rr->
            rr.update()
        }
    }

}