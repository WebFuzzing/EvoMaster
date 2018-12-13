package org.evomaster.core.problem.rest.serviceII

import org.evomaster.core.database.DbAction
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.problem.rest.RestIndividual
import org.evomaster.core.problem.rest.SampleType
import org.evomaster.core.problem.rest.serviceII.resources.RestResourceCalls
import org.evomaster.core.search.Action
import org.evomaster.core.search.Individual
import java.lang.IllegalArgumentException
import java.lang.IllegalStateException

class RestIndividualII(
        private val resourceCalls: MutableList<RestResourceCalls>,
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

    override fun canMutateStructure(): Boolean {
        return sampleType == SampleType.RANDOM ||
                sampleType == SampleType.SMART_GET_COLLECTION
    }

    override fun seeActions(): List<out Action> {
        if(resourceCalls.map { it.actions.size }.sum() != actions.size)
            throw  IllegalStateException("Mismatched between RestResourceCalls ${resourceCalls.map { it.actions.size }.sum()} and actions ${actions.size}")
        return actions
    }

    fun removeActionsFrom(last : Int) : Boolean{
        //individual is required to update only if last executed position is not last position
        if(last != actions.size -1){
            var loc = 0
            var rloc = 0
            resourceCalls.forEachIndexed { index, restResourceCalls ->
                loc += restResourceCalls.actions.size
                if(loc -1 >= last && rloc == 0){
                    rloc = index
                    if(loc - 1 > last){
                        (0 until (last - loc + 1)).forEach { restResourceCalls.actions.removeAt(restResourceCalls.actions.size - 1) }
                    }
                }
            }

            while(actions.size != last + 1){
                actions.removeAt(actions.size - 1)
            }

        }

        if(actions.size != resourceCalls.map { it.actions.size }.sum())
            throw IllegalStateException("invalid remove")
        return actions.size == resourceCalls.map { it.actions.size }.sum()
    }

    private fun updateActions() : Boolean{
        actions.clear()
        actions.addAll(resourceCalls.flatMap { it.actions })
        return actions.size == resourceCalls.map { it.actions.size }.sum()
    }

    fun removeResourceCall(position : Int) : Boolean{
        if(position > resourceCalls.size)
            throw IllegalArgumentException("position is out of range of list")
        resourceCalls.removeAt(position)
        return updateActions()
    }

    fun addResourceCall(position: Int, restCalls : RestResourceCalls) : Boolean{
        if(position == resourceCalls.size) resourceCalls.add(restCalls)
        resourceCalls.add(position, restCalls)
        return updateActions()
    }

    fun replaceResourceCall(position: Int, restCalls: RestResourceCalls) : Boolean{
        if(position > resourceCalls.size)
            throw IllegalArgumentException("position is out of range of list")
        resourceCalls.set(position, restCalls)
        return updateActions()
    }

    fun swapResourceCall(position1: Int, position2: Int) :Boolean{
        if(position1 > resourceCalls.size || position2 > resourceCalls.size)
            throw IllegalArgumentException("position is out of range of list")
        if(position1 == position2)
            throw IllegalArgumentException("It is not necessary to swap two same position on the resource call list")
        val first = resourceCalls[position1]
        resourceCalls.set(position1, resourceCalls[position2])
        resourceCalls.set(position2, first)
        return updateActions()
    }

    fun getResourceCalls() : List<RestResourceCalls>{
        return resourceCalls.toList()
    }

    fun getTemplate() : String{
        return actions.map { (it as RestCallAction).verb.toString() }.joinToString(HandleActionTemplate.SeparatorTemplate)
    }

}