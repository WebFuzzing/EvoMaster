package org.evomaster.core.problem.rest.serviceII

import org.evomaster.core.database.DbAction
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.problem.rest.RestIndividual
import org.evomaster.core.problem.rest.SampleType
import org.evomaster.core.problem.rest.serviceII.resources.RestResourceCalls
import org.evomaster.core.search.Action
import org.evomaster.core.search.Individual
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.service.tracer.TraceableElement
import java.lang.IllegalArgumentException
import java.lang.IllegalStateException

class RestIndividualII : RestIndividual {

    private val resourceCalls: MutableList<RestResourceCalls>

    constructor(restCalls: MutableList<RestResourceCalls>, sampleType: SampleType, dbInitialization: MutableList<DbAction>, description: String, traces : MutableList<RestIndividualII>): super(restCalls.flatMap { it.actions }.toMutableList(), sampleType, dbInitialization, description, traces){
        this.resourceCalls = restCalls
    }
    constructor(restCalls: MutableList<RestResourceCalls>, sampleType: SampleType, dbInitialization: MutableList<DbAction> = mutableListOf()): this(restCalls, sampleType, dbInitialization, sampleType.toString(), mutableListOf())
    constructor(restCalls: MutableList<RestResourceCalls>, sampleType: SampleType, description: String, traces: MutableList<RestIndividualII>): this(restCalls, sampleType, mutableListOf(), description, traces)

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
                sampleType == SampleType.SMART_GET_COLLECTION ||
                sampleType == SampleType.SMART_RESOURCE
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

    fun canModifyCall() : Boolean{
        return getResourceCalls().filter { it.resource.ar.templates.size > 1 }.isNotEmpty()
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
        return actions.map { (it as RestCallAction).verb.toString() }.joinToString(RTemplateHandler.SeparatorTemplate)
    }

    override fun seeGenesIdMap() : Map<Gene, String>{
        return resourceCalls.flatMap { r -> r.seeGenesIdMap().map { it.key to it.value } }.toMap()
    }



    override fun next(description : String) : TraceableElement?{
        if(isCapableOfTracking()){
            val copyTraces = mutableListOf<RestIndividualII>()
            if(!isRoot()){
                val size = getTrack()?.size?:0
                (0 until if(maxlength != -1 && size > maxlength - 1) maxlength-1  else size).forEach {
                    copyTraces.add(0, (getTrack()!![size-1-it] as RestIndividualII).copy() as RestIndividualII)
                }
            }
            copyTraces.add(this)
            return RestIndividualII(
                    resourceCalls.map { it.copy() }.toMutableList(),
                    sampleType,
                    dbInitialization.map { d -> d.copy() as DbAction } as MutableList<DbAction>,
                    description,
                    copyTraces)
        }
        return copy()
    }

    override fun copy(withTrack: Boolean): RestIndividualII {
        when(withTrack){
            false-> return copy() as RestIndividualII
            else ->{
                getTrack()?:return copy() as RestIndividualII
                val copyTraces = mutableListOf<RestIndividualII>()
                getTrack()?.forEach {
                    copyTraces.add((it as RestIndividualII).copy() as RestIndividualII)
                }
                return RestIndividualII(
                        resourceCalls.map { it.copy() }.toMutableList(),
                        sampleType,
                        dbInitialization.map { d -> d.copy() as DbAction } as MutableList<DbAction>,
                        getDescription(),
                        copyTraces
                )
            }
        }
    }
}