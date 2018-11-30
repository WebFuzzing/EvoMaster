package org.evomaster.core.problem.rest2

import com.google.inject.Inject
import org.evomaster.core.problem.rest.serviceII.RestIndividualII
import org.evomaster.core.problem.rest.serviceII.RestSamplerII
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Individual
import org.evomaster.core.search.service.StructureMutator

class RestResourceStructureMutator : StructureMutator() {

    @Inject
    protected lateinit var sampler: RestSamplerII

    override fun mutateStructure(individual: Individual) {
        if(individual !is RestIndividualII)
            throw IllegalArgumentException("Invalid individual type")

        //TODO Man
        if (!individual.canMutateStructure()) {
            return //if it is not mutable, it should not enter the structure mutator
        }
    }

    private fun mutateRestResourceCalls(ind: RestIndividualII) {
        val num = ind.resourceCalls.size
        val type = randomness.choose(MutationType.values().filter { it.size >= num }.filterNot{ ind.seeActions().size == config.maxTestSize && it == MutationType.ADD })
        when(type){
            MutationType.ADD -> handleAdd(ind)
            MutationType.DELETE -> handleDelete(ind)
            MutationType.SWAP -> handleSwap(ind)
            MutationType.REPLACE -> handleReplace(ind)
        }
    }

    enum class MutationType(val size: Int){
        DELETE(2),
        SWAP(2),
        ADD(1),
        REPLACE(1)
    }

    private fun handleDelete(ind: RestIndividualII){
        val pos = randomness.choose(ind.resourceCalls)
        ind.resourceCalls.remove(pos)
    }

    private fun handleSwap(ind: RestIndividualII){
        val cands = randomness.choose(Array(ind.resourceCalls.size){i -> i}.toList(), 2)
        val first = ind.resourceCalls[cands[0]]
        ind.resourceCalls.set(cands[0], ind.resourceCalls[cands[1]])
        ind.resourceCalls.set(cands[1], first)
    }

    private fun handleAdd(ind: RestIndividualII){
        var max = config.maxTestSize
        ind.resourceCalls.forEach { max -= it.actions.size }
        val call = sampler.handleAddResource(ind, max)
        val pos = randomness.nextInt(0, ind.resourceCalls.size)
        if(pos == ind.resourceCalls.size) ind.resourceCalls.add(call)
        else ind.resourceCalls.add(pos, call)
    }

    private fun handleReplace(ind: RestIndividualII){
        val pos = randomness.nextInt(0, ind.resourceCalls.size-1)
        val old = ind.resourceCalls[pos]
        var max = config.maxTestSize
        ind.resourceCalls.forEach { max -= it.actions.size }
        max += ind.resourceCalls[pos].actions.size
        var new = old.resource.ar.generateAnother(old, randomness, max)
        if(new == null){
            new = old.resource.ar.sampleOneAction(null, randomness, max)
        }

        assert(new != null)
        ind.resourceCalls.set(pos, new)
    }

    //copy from RestStructureMutator
    override fun addInitializingActions(individual: EvaluatedIndividual<*>) {
        if (!config.shouldGenerateSqlData()) {
            return
        }
    }

}