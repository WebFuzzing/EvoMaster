package org.evomaster.core.problem.rest.service

import com.google.inject.Inject
import org.evomaster.core.database.DbActionUtils
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.problem.rest.RestIndividual
import org.evomaster.core.problem.rest.auth.AuthenticationInfo
import org.evomaster.core.problem.rest.resource.RestResourceCalls
import org.evomaster.core.search.Action
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Individual
import org.evomaster.core.search.gene.sql.SqlForeignKeyGene
import org.evomaster.core.search.gene.sql.SqlPrimaryKeyGene
import org.evomaster.core.search.service.mutator.MutatedGeneSpecification

class RestResourceStructureMutator : AbstractRestStructureMutator() {

    @Inject
    private lateinit var rm : ResourceManageService

    @Inject
    private lateinit var dm : ResourceDepManageService

    @Inject
    private lateinit var sampler : ResourceSampler

    override fun mutateStructure(individual: Individual, mutatedGenes: MutatedGeneSpecification?) {
        if(individual !is RestIndividual)
            throw IllegalArgumentException("Invalid individual type")

        mutateRestResourceCalls(individual, mutatedGenes = mutatedGenes)
        if (config.trackingEnabled()) tag(individual, time.evaluatedIndividuals)
    }

    fun mutateRestResourceCalls(ind: RestIndividual, specified : MutationType?=null, mutatedGenes: MutatedGeneSpecification? = null) {

        val executedStructureMutator = specified?:
            randomness.choose(getAvailableMutator(ind) )

        when(executedStructureMutator){
            MutationType.ADD -> handleAdd(ind, mutatedGenes)
            MutationType.DELETE -> handleDelete(ind, mutatedGenes)
            MutationType.SWAP -> handleSwap(ind, mutatedGenes)
            MutationType.REPLACE -> handleReplace(ind, mutatedGenes)
            MutationType.MODIFY -> handleModify(ind, mutatedGenes)
            MutationType.SQL_REMOVE -> handleRemoveSQL(ind, mutatedGenes)
            MutationType.SQL_ADD -> handleAddSQL(ind, mutatedGenes)
        }

        ind.repairDBActions(rm.getSqlBuilder(), randomness)
    }

    private fun getAvailableMutator(ind: RestIndividual) : List<MutationType>{
        val num = ind.getResourceCalls().size
        val sqlNum = ind.seeResource(RestIndividual.ResourceFilter.ONLY_SQL_INSERTION).size
        return MutationType.values()
            .filter {  num >= it.minSize && sqlNum >= it.minSQLSize}
            .filterNot {
                // if there is no db or sql resource handling is not enabled, SQL_REMOVE and SQL_ALL are not applicable
                ((config.maxSqlInitActionsPerResource == 0 || rm.getTableInfo().isEmpty()) && (it == MutationType.SQL_ADD || it == MutationType.SQL_REMOVE) ) ||
                        // if there is no dbInitialization, SQL_REMOVE is not applicable
                        (ind.dbInitialization.isEmpty() && it == MutationType.SQL_REMOVE)
            }
            .filterNot{
                (ind.seeActions().size == config.maxTestSize && it == MutationType.ADD) ||
                        //if the individual includes all resources, ADD and REPLACE are not applicable
                        (ind.getResourceCalls().map {
                            it.resourceInstance?.getKey()
                        }.toSet().size >= rm.getResourceCluster().size && (it == MutationType.ADD || it == MutationType.REPLACE)) ||
                        //if the size of deletable individual is less 2, Delete and SWAP are not applicable
                        (ind.getResourceCalls().filter(RestResourceCalls::isDeletable).size < 2 && (it == MutationType.DELETE || it == MutationType.SWAP))
            }
    }

    /**
     * the class defines possible methods to mutate ResourceRestIndividual regarding its resources
     * @param minSize is a minimum number of rest actions in order to apply the mutation
     * @param minSQLSize is a minimum number of db actions in order to apply the mutation
     */
    enum class MutationType(val minSize: Int, val minSQLSize : Int = 0){
        DELETE(2),
        SWAP(2),
        ADD(1),
        REPLACE(1),
        MODIFY(1),
        SQL_REMOVE(1, 1),
        SQL_ADD(1, 1)
    }

    /**
     * add resources with SQL to [ind]
     * a number of resources to be added is related to EMConfig.maxSqlInitActionsPerResource
     */
    private fun handleAddSQL(ind: RestIndividual, mutatedGenes: MutatedGeneSpecification?){
        val numOfResource = randomness.nextInt(1, config.maxSqlInitActionsPerResource)
        val added = if (doesApplyDependencyHeuristics()) dm.addRelatedSQL(ind, numOfResource)
                    else dm.createDbActions(randomness.choose(rm.getTableInfo().keys),numOfResource)

        DbActionUtils.repairFkForInsertions(added)

        if(config.isEnabledArchiveSolution()){
            TODO("update impacts")
        }

        mutatedGenes?.addedDbActions?.addAll(added)
        ind.dbInitialization.addAll(added)
    }

    /**
     * remove one resource which are created by SQL
     *
     * Man: shall we remove SQLs which represents existing data?
     * It might be useful to reduce the useless db genes.
     */
    private fun handleRemoveSQL(ind: RestIndividual, mutatedGenes: MutatedGeneSpecification?){
        // remove unrelated tables
        var candidates = if (doesApplyDependencyHeuristics()) dm.unRelatedSQL(ind) else ind.dbInitialization

        if (candidates.isEmpty())
            candidates = ind.dbInitialization

        val remove = randomness.choose(candidates)

        val pks = remove.seeGenes().flatMap { it.flatView() }.filterIsInstance<SqlPrimaryKeyGene>()
        if (pks.isNotEmpty()){
            val removeDbFKs = ind.dbInitialization.subList(ind.dbInitialization.indexOf(remove)+1, ind.dbInitialization.size).filter {
                it.seeGenes().flatMap { g-> g.flatView() }.filterIsInstance<SqlForeignKeyGene>()
                    .any {fk-> pks.any {pk->fk.uniqueIdOfPrimaryKey == pk.uniqueId} } }

            ind.dbInitialization.remove(remove)
            ind.dbInitialization.removeAll(removeDbFKs)
            mutatedGenes?.removedDbActions?.add(remove)
            mutatedGenes?.removedDbActions?.addAll(removeDbFKs)
        }else{
            ind.dbInitialization.remove(remove)
            mutatedGenes?.removedDbActions?.add(remove)
        }


        if(config.isEnabledArchiveSolution()){
            TODO("update impacts")
        }
    }

    private fun doesApplyDependencyHeuristics() : Boolean{
        return dm.isDependencyNotEmpty()
                && randomness.nextBoolean(config.probOfEnablingResourceDependencyHeuristics)
    }

    /**
     * delete one resource call
     */
    private fun handleDelete(ind: RestIndividual, mutatedGenes: MutatedGeneSpecification?){

        val fromDependency = doesApplyDependencyHeuristics()

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
        val fromDependency = doesApplyDependencyHeuristics()

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
            val position = (ind.getResourceCalls().indices).toMutableList()
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

        val fromDependency = doesApplyDependencyHeuristics()

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

        val fromDependency = doesApplyDependencyHeuristics()

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

        val pair = if(fromDependency && pos != ind.getResourceCalls().size -1){
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
        var new = old.getResourceNode().generateAnother(old, randomness, max, sqlInsertBuilder = rm.getSqlBuilder())
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
        if (!config.shouldGenerateSqlData()) {
            return
        }

        val ind = individual.individual as? RestIndividual
            ?: throw IllegalArgumentException("Invalid individual type")

        val fw = individual.fitness.getViewOfAggregatedFailedWhere()
            //TODO likely to remove/change once we ll support VIEWs
            .filter { sampler.canInsertInto(it.key) }

        if (fw.isEmpty()) {
            return
        }

        val old = mutableListOf<Action>().plus(ind.seeInitializingActions())

        val addedInsertions = handleFailedWhereSQL(ind, fw, mutatedGenes, sampler)

        ind.repairInitializationActions(randomness)
        // update impact based on added genes
        if(mutatedGenes != null && config.isEnabledArchiveGeneSelection()){
            individual.updateImpactGeneDueToAddedInitializationGenes(
                mutatedGenes,
                old,
                addedInsertions
            )
        }
    }

    private fun maintainAuth(authInfo: AuthenticationInfo?, mutated: RestResourceCalls){
        authInfo?.let { auth->
            mutated.actions.forEach { if(it is RestCallAction) it.auth = auth }
        }
    }

}