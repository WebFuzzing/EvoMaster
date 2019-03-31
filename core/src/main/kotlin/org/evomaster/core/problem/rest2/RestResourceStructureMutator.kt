package org.evomaster.core.problem.rest2

import com.google.inject.Inject
import org.evomaster.core.problem.rest.serviceII.RestIndividualII
import org.evomaster.core.problem.rest.serviceII.RestSamplerII
import org.evomaster.core.problem.rest2.resources.ResourceManageService
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Individual
import org.evomaster.core.search.service.mutator.StructureMutator

class RestResourceStructureMutator : StructureMutator() {

    @Inject
    private lateinit var sampler: RestSamplerII

    @Inject
    private lateinit var rm : ResourceManageService


    override fun mutateStructure(individual: Individual) {
        if(individual !is RestIndividualII)
            throw IllegalArgumentException("Invalid individual type")

        mutateRestResourceCalls(individual)
    }

    private fun mutateRestResourceCalls(ind: RestIndividualII) {

        val num = ind.getResourceCalls().size
        val type = randomness.choose(MutationType.values().filter {  num >= it.size }
                .filterNot{
                    (ind.seeActions().size == config.maxTestSize && it == MutationType.ADD) ||
                            (ind.getResourceCalls().map {
                                //it.resource.getAResourceKey()
                                it.resource.getKey()
                            }.toSet().size >= rm.getResourceCluster().size && (it == MutationType.ADD || it == MutationType.REPLACE)) ||
                            (!ind.canModifyCall() && it == MutationType.MODIFY)
                })
        if(config.enableTrackEvaluatedIndividual || config.enableTrackIndividual) ind.appendDescription(type.toString())
        when(type){
            MutationType.ADD -> handleAdd(ind)
            MutationType.DELETE -> handleDelete(ind)
            MutationType.SWAP -> handleSwap(ind)
            MutationType.REPLACE -> handleReplace(ind)
            MutationType.MODIFY -> handleModify(ind)
        }
    }

    enum class MutationType(val size: Int){
        DELETE(2),
        SWAP(2),
        ADD(1),
        REPLACE(1),
        MODIFY(1)
    }

    private fun handleDelete(ind: RestIndividualII){
        val pos = randomness.nextInt(0, ind.getResourceCalls().size-1)
        ind.removeResourceCall(pos)

    }

    private fun handleSwap(ind: RestIndividualII){
        val cands = randomness.choose(Array(ind.getResourceCalls().size){i -> i}.toList(), 2)
        ind.swapResourceCall(cands[0], cands[1])
    }

    private fun handleAdd(ind: RestIndividualII){

        var max = config.maxTestSize
        ind.getResourceCalls().forEach { max -= it.actions.size }

        val fromDependency = rm.isDependencyNotEmpty()
                && randomness.nextBoolean(config.probOfEnablingResourceDependencyHeuristics)

        var call = if(fromDependency){
                        rm.handleAddDepResource(ind, max)
                    }else null

        if(call == null){
            call =  rm.handleAddResource(ind, max)
            val pos = randomness.nextInt(0, ind.getResourceCalls().size)
            ind.addResourceCall(pos, call)
        }else{
            rm.bindCallWithFront(call, ind.getResourceCalls().toMutableList())

            //if call is to create new resource, and the related resource is not related to any resource, it might need to put the call in the front of ind,
            //else add last position if it has dependency with existing resources
            val pos = if(ind.getResourceCalls().filter { !it.template.independent }.isNotEmpty())
                ind.getResourceCalls().size
            else
                0

            ind.addResourceCall( pos, call)
        }


    }

    private fun handleReplace(ind: RestIndividualII){
        var max = config.maxTestSize
        ind.getResourceCalls().forEach { max -= it.actions.size }

        val call = rm.handleAddResource(ind, max)

        val pos = randomness.nextInt(0, ind.getResourceCalls().size - 1)
        ind.replaceResourceCall(pos, call)
    }

    private fun handleModify(ind: RestIndividualII){
        val pos = randomness.nextInt(0, ind.getResourceCalls().size-1)
        val old = ind.getResourceCalls()[pos]
        var max = config.maxTestSize
        ind.getResourceCalls().forEach { max -= it.actions.size }
        max += ind.getResourceCalls()[pos].actions.size
        var new = old.resource.ar.generateAnother(old, randomness, max)
        if(new == null){
            new = old.resource.ar.sampleOneAction(null, randomness, max)
        }
        assert(new != null)
        ind.replaceResourceCall(pos, new)
    }

    //copy from RestStructureMutator
    override fun addInitializingActions(individual: EvaluatedIndividual<*>) {
        if (!config.shouldGenerateSqlData()) {
            return
        }
    }

}