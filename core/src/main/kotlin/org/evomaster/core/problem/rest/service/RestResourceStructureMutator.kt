package org.evomaster.core.problem.rest.service

import com.google.inject.Inject
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.problem.rest.RestIndividual
import org.evomaster.core.problem.rest.auth.AuthenticationInfo
import org.evomaster.core.problem.rest.resource.RestResourceCalls
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Individual
import org.evomaster.core.search.service.mutator.MutatedGeneSpecification
import org.evomaster.core.search.service.mutator.StructureMutator

class RestResourceStructureMutator : StructureMutator() {

    @Inject
    private lateinit var rm : ResourceManageService

    @Inject
    private lateinit var dm : ResourceDepManageService

    //var executedStructureMutator :MutationType? = null

    override fun mutateStructure(individual: Individual, mutatedGenes: MutatedGeneSpecification?) {
        if(individual !is RestIndividual)
            throw IllegalArgumentException("Invalid individual type")

        mutateRestResourceCalls(individual, mutatedGenes = mutatedGenes)
    }

    fun mutateRestResourceCalls(ind: RestIndividual, specified : MutationType?=null, mutatedGenes: MutatedGeneSpecification? = null) {

        val num = ind.getResourceCalls().size

        val validCandidates =  MutationType.values()
                .filter {  num >= it.minSize }
                .filterNot{
                    (ind.seeActions().size == config.maxTestSize && it == MutationType.ADD) ||
                            //if the individual includes all resources, ADD and REPLACE are not applicable
                            (ind.getResourceCalls().map {
                                it.resourceInstance?.getKey()
                            }.toSet().size >= rm.getResourceCluster().size && (it == MutationType.ADD || it == MutationType.REPLACE)) ||
                            //if the size of deletable individual is less 2, Delete and SWAP are not applicable
                            (ind.getResourceCalls().filter(RestResourceCalls::isDeletable).size < 2 && (it == MutationType.DELETE || it == MutationType.SWAP))
                }
        val executedStructureMutator = specified?:
            randomness.choose(validCandidates )
        when(executedStructureMutator){
            MutationType.ADD -> handleAdd(ind, mutatedGenes)
            MutationType.DELETE -> handleDelete(ind, mutatedGenes)
            MutationType.SWAP -> handleSwap(ind, mutatedGenes)
            MutationType.REPLACE -> handleReplace(ind, mutatedGenes)
            MutationType.MODIFY -> handleModify(ind, mutatedGenes)
        }

        ind.repairDBActions(rm.getSqlBuilder())
    }

    /**
     * the class defines possible methods to mutate ResourceRestIndividual regarding its resources
     */
    enum class MutationType(val minSize: Int){
        DELETE(2),
        SWAP(2),
        ADD(1),
        REPLACE(1),
        MODIFY(1)
    }

    /**
     * delete one resource call
     */
    private fun handleDelete(ind: RestIndividual, mutatedGenes: MutatedGeneSpecification?){

        val fromDependency = dm.isDependencyNotEmpty()
                && randomness.nextBoolean(config.probOfEnablingResourceDependencyHeuristics)

        val removed = if(fromDependency){
                dm.handleDelNonDepResource(ind)
            }else null

        val pos = if(removed != null) ind.getResourceCalls().indexOf(removed)
            else ind.getResourceCalls().indexOf(randomness.choose(ind.getResourceCalls().filter(RestResourceCalls::isDeletable)))
        //randomness.nextInt(0, ind.getResourceCalls().size - 1)

        mutatedGenes?.removedGene?.addAll(ind.getResourceCalls()[pos].actions.flatMap { it.seeGenes() })
        mutatedGenes?.mutatedPosition?.add(pos)

        ind.removeResourceCall(pos)
    }

    /**
     * swap two resource calls
     */
    private fun handleSwap(ind: RestIndividual, mutatedGenes: MutatedGeneSpecification?){
        val fromDependency = dm.isDependencyNotEmpty()
                && randomness.nextBoolean(config.probOfEnablingResourceDependencyHeuristics)

        if(fromDependency){
            val pair = dm.handleSwapDepResource(ind)
            if(pair!=null){
                ind.swapResourceCall(pair.first, pair.second)
                mutatedGenes?.mutatedPosition?.add(pair.first)
                mutatedGenes?.mutatedPosition?.add(pair.second)
                return
            }
        }

        if(config.probOfEnablingResourceDependencyHeuristics > 0.0){
            val position = (0 until ind.getResourceCalls().size).toMutableList()
            while (position.isNotEmpty()){
                val chosen = randomness.choose(position)
                if(ind.isMovable(chosen)) {
                    val moveTo = randomness.choose(ind.getMovablePosition(chosen))
                    if(chosen < moveTo) ind.swapResourceCall(chosen, moveTo)
                    else ind.swapResourceCall(moveTo, chosen)

                    mutatedGenes?.mutatedPosition?.add(moveTo)
                    mutatedGenes?.mutatedPosition?.add(chosen)
                    return
                }
                position.remove(chosen)
            }
            throw IllegalStateException("the individual cannot apply swap mutator!")
        }else{
            val candidates = randomness.choose(Array(ind.getResourceCalls().size){i -> i}.toList(), 2)
            mutatedGenes?.mutatedPosition?.add(candidates[0])
            mutatedGenes?.mutatedPosition?.add(candidates[1])
            ind.swapResourceCall(candidates[0], candidates[1])
        }
    }

    /**
     * add new resource call
     *
     * Note that if dependency is enabled,
     * the added resource can be its dependent resource with a probability i.e.,[config.probOfEnablingResourceDependencyHeuristics]
     */
    private fun handleAdd(ind: RestIndividual, mutatedGenes: MutatedGeneSpecification?){
        val auth = ind.seeActions().filterIsInstance<RestCallAction>().map { it.auth }.run {
            if (isEmpty()) null
            else randomness.choose(this)
        }

        val sizeOfCalls = ind.getResourceCalls().size

        var max = config.maxTestSize
        ind.getResourceCalls().forEach { max -= it.actions.size }
        if (max == 0){
            handleDelete(ind, mutatedGenes)
            return
        }

        val fromDependency = dm.isDependencyNotEmpty()
                && randomness.nextBoolean(config.probOfEnablingResourceDependencyHeuristics)

        val pair = if(fromDependency){
                        dm.handleAddDepResource(ind, max)
                    }else null

        if(pair == null){
            val randomCall =  rm.handleAddResource(ind, max)
            val pos = randomness.nextInt(0, ind.getResourceCalls().size)

            mutatedGenes?.addedGenes?.addAll(randomCall.actions.flatMap { it.seeGenes() })
            mutatedGenes?.mutatedPosition?.add(pos)

            maintainAuth(auth, randomCall)
            ind.addResourceCall(pos, randomCall)
        }else{
            var addPos : Int? = null
            if(pair.first != null){
                val pos = ind.getResourceCalls().indexOf(pair.first!!)
                dm.bindCallWithFront(pair.first!!, mutableListOf(pair.second))
                addPos = randomness.nextInt(0, pos)
            }
            if (addPos == null) addPos = randomness.nextInt(0, ind.getResourceCalls().size)

            //if call is to create new resource, and the related resource is not related to any resource, it might need to put the call in the front of ind,
            //else add last position if it has dependency with existing resources
//            val pos = if(ind.getResourceCalls().filter { !it.template.independent }.isNotEmpty())
//                ind.getResourceCalls().size
//            else
//                0

            mutatedGenes?.addedGenes?.addAll(pair.second.actions.flatMap { it.seeGenes() })
            mutatedGenes?.mutatedPosition?.add(addPos)

            maintainAuth(auth, pair.second)
            ind.addResourceCall( addPos, pair.second)
        }

        assert(sizeOfCalls == ind.getResourceCalls().size - 1)
    }

    /**
     * replace one of resource call with other resource
     */
    private fun handleReplace(ind: RestIndividual, mutatedGenes: MutatedGeneSpecification?){
        val auth = ind.seeActions().filterIsInstance<RestCallAction>().map { it.auth }.run {
            if (isEmpty()) null
            else randomness.choose(this)
        }

        var max = config.maxTestSize
        ind.getResourceCalls().forEach { max -= it.actions.size }

        val fromDependency = dm.isDependencyNotEmpty()
                && randomness.nextBoolean(config.probOfEnablingResourceDependencyHeuristics)

        var pos = if(fromDependency){
            dm.handleDelNonDepResource(ind)?.run {
                ind.getResourceCalls().indexOf(this)
            }
        }else{
            null
        }
        if(pos == null)
            pos = ind.getResourceCalls().indexOf(randomness.choose(ind.getResourceCalls().filter(RestResourceCalls::isDeletable)))
            //randomness.nextInt(0, ind.getResourceCalls().size - 1)

        max += ind.getResourceCalls()[pos].actions.size

        var pair = if(fromDependency && pos != ind.getResourceCalls().size -1){
                        dm.handleAddDepResource(ind, max, if (pos == ind.getResourceCalls().size-1) mutableListOf() else ind.getResourceCalls().subList(pos+1, ind.getResourceCalls().size).toMutableList())
                    }else null


        var call = pair?.second
        if(pair == null){
            call =  rm.handleAddResource(ind, max)
        }else{
            //rm.bindCallWithFront(call, ind.getResourceCalls().toMutableList())
            if(pair.first != null){
                dm.bindCallWithFront(pair.first!!, mutableListOf(pair.second))
            }
        }
        mutatedGenes?.removedGene?.addAll(ind.getResourceCalls()[pos].actions.flatMap { it.seeGenes() })
        mutatedGenes?.mutatedPosition?.add(pos)
        ind.removeResourceCall(pos)

        maintainAuth(auth, call!!)
        mutatedGenes?.addedGenes?.addAll(call!!.actions.flatMap { it.seeGenes() })
        ind.addResourceCall(pos, call)
    }

    /**
     *  modify one of resource call with other template
     */
    private fun handleModify(ind: RestIndividual, mutatedGenes: MutatedGeneSpecification?){
        val auth = ind.seeActions().filterIsInstance<RestCallAction>().map { it.auth }.run {
            if (isEmpty()) null
            else randomness.choose(this)
        }

        val pos = randomness.nextInt(0, ind.getResourceCalls().size-1)
        val old = ind.getResourceCalls()[pos]
        var max = config.maxTestSize
        ind.getResourceCalls().forEach { max -= it.actions.size }
        max += ind.getResourceCalls()[pos].actions.size
        var new = old.getResourceNode().generateAnother(old, randomness, max)
        if(new == null){
            new = old.getResourceNode().sampleOneAction(null, randomness)
        }
        maintainAuth(auth, new)

        mutatedGenes?.removedGene?.addAll(ind.getResourceCalls()[pos].actions.flatMap { it.seeGenes() })
        mutatedGenes?.addedGenes?.addAll(new!!.actions.flatMap { it.seeGenes() })
        mutatedGenes?.mutatedPosition?.add(pos)

        ind.replaceResourceCall(pos, new)
    }

    /**
     * for ResourceRestIndividual, dbaction(s) has been distributed to each resource call [ResourceRestCalls]
     */
    override fun addInitializingActions(individual: EvaluatedIndividual<*>, mutatedGenes: MutatedGeneSpecification?) {
        //do noting
    }

    private fun maintainAuth(authInfo: AuthenticationInfo?, mutated: RestResourceCalls){
        authInfo?.let { auth->
            mutated.actions.forEach { if(it is RestCallAction) it.auth = auth }
        }
    }

}