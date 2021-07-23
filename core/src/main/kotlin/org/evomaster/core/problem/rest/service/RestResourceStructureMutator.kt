package org.evomaster.core.problem.rest.service

import com.google.inject.Inject
import org.evomaster.core.database.DbAction
import org.evomaster.core.problem.httpws.service.HttpWsStructureMutator
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.problem.rest.RestIndividual
import org.evomaster.core.problem.httpws.service.auth.AuthenticationInfo
import org.evomaster.core.problem.rest.resource.RestResourceCalls
import org.evomaster.core.search.Action
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Individual
import org.evomaster.core.search.ActionFilter
import org.evomaster.core.search.ActionFilter.*
import org.evomaster.core.search.gene.sql.SqlForeignKeyGene
import org.evomaster.core.search.gene.sql.SqlPrimaryKeyGene
import org.evomaster.core.search.service.mutator.MutatedGeneSpecification
import kotlin.math.max
import kotlin.math.min
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class RestResourceStructureMutator : HttpWsStructureMutator() {

    @Inject
    private lateinit var rm : ResourceManageService

    @Inject
    private lateinit var dm : ResourceDepManageService

    @Inject
    private lateinit var sampler : ResourceSampler

    companion object{
        private val log : Logger = LoggerFactory.getLogger(RestResourceStructureMutator::class.java)
    }

    override fun mutateStructure(individual: Individual, mutatedGenes: MutatedGeneSpecification?) {
        if(individual !is RestIndividual)
            throw IllegalArgumentException("Invalid individual type")

        mutateRestResourceCalls(individual, mutatedGenes = mutatedGenes)
        if (config.trackingEnabled()) tag(individual, time.evaluatedIndividuals)
    }

    override fun canApplyStructureMutator(individual: Individual): Boolean {
        if(individual !is RestIndividual)
            throw IllegalArgumentException("Invalid individual type")

        return super.canApplyStructureMutator(individual) && getAvailableMutator(individual).isNotEmpty()
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
    }

    private fun getAvailableMutator(ind: RestIndividual) : List<MutationType>{
        val num = ind.getResourceCalls().size
        val sqlNum = ind.seeResource(RestIndividual.ResourceFilter.ONLY_SQL_INSERTION).size
        return MutationType.values()
            .filter {  num >= it.minSize && sqlNum >= it.minSQLSize && isMutationTypeApplicable(it, ind)}

    }

    private fun isMutationTypeApplicable(type: MutationType, ind : RestIndividual): Boolean{
        val delSize = ind.getResourceCalls().filter(RestResourceCalls::isDeletable).size
        return when(type){
            MutationType.ADD -> ind.seeActions().size < config.maxTestSize && !rm.cluster.doesCoverAll(ind)
            MutationType.SWAP -> ind.extractSwapCandidates().isNotEmpty()
            MutationType.REPLACE -> !rm.cluster.doesCoverAll(ind) && delSize > 0
            MutationType.DELETE -> delSize > 0 && ind.getResourceCalls().size >=2
            MutationType.SQL_ADD -> config.maxSqlInitActionsPerResource != 0 && rm.getTableInfo().isNotEmpty()
            MutationType.SQL_REMOVE -> config.maxSqlInitActionsPerResource != 0 && rm.getTableInfo().isNotEmpty() && ind.seeInitializingActions().isNotEmpty()
            MutationType.MODIFY -> delSize > 0
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
        if (config.maxSqlInitActionsPerResource == 0)
            throw IllegalStateException("this method should not be invoked when config.maxSqlInitActionsPerResource is 0")
        val numOfResource = randomness.nextInt(1, rm.getSqlMaxNumOfResource())
        val added = if (doesApplyDependencyHeuristics()) dm.addRelatedSQL(ind, numOfResource)
                    else dm.createDbActions(randomness.choose(rm.getTableInfo().keys),numOfResource)

        ind.addInitializingActions(actions = added.flatten())
        mutatedGenes?.addedDbActions?.addAll(added)
    }

    /**
     * remove one resource which are created by SQL
     *
     * Man: shall we remove SQLs which represents existing data?
     * It might be useful to reduce the useless db genes.
     */
    private fun handleRemoveSQL(ind: RestIndividual, mutatedGenes: MutatedGeneSpecification?){
        // remove unrelated tables
        var candidates = if (doesApplyDependencyHeuristics()) dm.unRelatedSQL(ind) else ind.seeInitializingActions()

        if (candidates.isEmpty())
            candidates = ind.seeInitializingActions()

        val num = randomness.nextInt(1, max(1, min(rm.getSqlMaxNumOfResource(), candidates.size -1)))
        val remove = randomness.choose(candidates, num)
        val relatedRemove = mutableListOf<DbAction>()
        relatedRemove.addAll(remove)
        remove.forEach {
            getRelatedRemoveDbActions(ind, it, relatedRemove)
        }
        val set = relatedRemove.toSet().toMutableList()
        mutatedGenes?.removedDbActions?.addAll(set.map { it to ind.seeInitializingActions().indexOf(it) })
        ind.removeAll(set)
    }

    private fun getRelatedRemoveDbActions(ind: RestIndividual, remove : DbAction, relatedRemove: MutableList<DbAction>){
        val pks = remove.seeGenes().flatMap { it.flatView() }.filterIsInstance<SqlPrimaryKeyGene>()
        val index = ind.seeInitializingActions().indexOf(remove)
        if (index < ind.seeInitializingActions().size - 1 && pks.isNotEmpty()){
            val removeDbFKs = ind.seeInitializingActions().subList(index + 1, ind.seeInitializingActions().size).filter {
                it.seeGenes().flatMap { g-> g.flatView() }.filterIsInstance<SqlForeignKeyGene>()
                    .any {fk-> pks.any {pk->fk.uniqueIdOfPrimaryKey == pk.uniqueId} } }
            relatedRemove.addAll(removeDbFKs)
            removeDbFKs.forEach {
                getRelatedRemoveDbActions(ind, it, relatedRemove)
            }
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

        val removedActions = ind.getResourceCalls()[pos].seeActions(ActionFilter.ALL)
        removedActions.forEach {
            mutatedGenes?.addRemovedOrAddedByAction(
                it,
                ind.seeActions(ActionFilter.NO_INIT).indexOf(it),
                true,
                resourcePosition = pos
            )
        }

        ind.removeResourceCall(pos)
    }

    /**
     * swap two resource calls
     */
    private fun handleSwap(ind: RestIndividual, mutatedGenes: MutatedGeneSpecification?){
        val candidates = ind.extractSwapCandidates()

        if (candidates.isEmpty()){
            throw IllegalStateException("the individual cannot apply swap mutator!")
        }

        val fromDependency = doesApplyDependencyHeuristics()

        if(fromDependency){
            val pair = dm.handleSwapDepResource(ind, candidates)
            if(pair!=null){
                mutatedGenes?.swapAction(pair.first, ind.getActionIndexes(NO_INIT, pair.first), ind.getActionIndexes(NO_INIT, pair.second))
                ind.swapResourceCall(pair.first, pair.second)
                return
            }
        }

        val randPair = randomizeSwapCandidates(candidates)
        val chosen = randPair.first
        val moveTo = randPair.second
        mutatedGenes?.swapAction(moveTo, ind.getActionIndexes(NO_INIT, chosen), ind.getActionIndexes(NO_INIT, moveTo))
        if(chosen < moveTo) ind.swapResourceCall(chosen, moveTo)
        else ind.swapResourceCall(moveTo, chosen)

    }

    private fun randomizeSwapCandidates(candidates: Map<Int, Set<Int>>): Pair<Int, Int>{
        return randomness.choose(candidates.keys).run {
            this to randomness.choose(candidates[this]!!)
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
        ind.getResourceCalls().forEach { max -= it.seeActions(NO_SQL).size }
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

            maintainAuth(auth, randomCall)
            ind.addResourceCall(pos, randomCall)

            randomCall.seeActions(ALL).forEach {
                mutatedGenes?.addRemovedOrAddedByAction(
                    it,
                    ind.seeActions(NO_INIT).indexOf(it),
                    false,
                    resourcePosition = pos
                )
            }

        }else{
            var addPos : Int? = null
            if(pair.first != null){
                val pos = ind.getResourceCalls().indexOf(pair.first!!)
                pair.first!!.bindWithOtherRestResourceCalls(mutableListOf(pair.second), rm.cluster,true)
                addPos = randomness.nextInt(0, pos)
            }
            if (addPos == null) addPos = randomness.nextInt(0, ind.getResourceCalls().size)

            maintainAuth(auth, pair.second)
            ind.addResourceCall( addPos, pair.second)

            pair.second.apply {
                seeActions(ALL).forEach {
                    mutatedGenes?.addRemovedOrAddedByAction(
                        it,
                        ind.seeActions(ActionFilter.NO_INIT).indexOf(it),
                        false,
                        resourcePosition = addPos
                    )
                }
            }
        }

        assert(sizeOfCalls == ind.getResourceCalls().size - 1)
    }

    /**
     * replace one of resource call with other resource
     */
    private fun handleReplace(ind: RestIndividual, mutatedGenes: MutatedGeneSpecification?){
        val auth = ind.seeActions().map { it.auth }.run {
            if (isEmpty()) null
            else randomness.choose(this)
        }

        var max = config.maxTestSize
        ind.getResourceCalls().forEach { max -= it.seeActionSize(NO_SQL) }

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

        max += ind.getResourceCalls()[pos].seeActionSize(NO_SQL)

        val pair = if(fromDependency && pos != ind.getResourceCalls().size -1){
                        dm.handleAddDepResource(ind, max, if (pos == ind.getResourceCalls().size-1) mutableListOf() else ind.getResourceCalls().subList(pos+1, ind.getResourceCalls().size).toMutableList())
                    }else null

        var call = pair?.second
        if(pair == null){
            call =  rm.handleAddResource(ind, max)
        }else{
            if(pair.first != null){
                pair.first!!.bindWithOtherRestResourceCalls(mutableListOf(pair.second), rm.cluster,true)
            }
        }

       ind.getResourceCalls()[pos].seeActions(ALL).forEach {
           mutatedGenes?.addRemovedOrAddedByAction(
               it,
               ind.seeActions(NO_INIT).indexOf(it),
               true,
               resourcePosition = pos
           )
       }

        ind.removeResourceCall(pos)

        maintainAuth(auth, call!!)
        ind.addResourceCall(pos, call)

        call.seeActions(ALL).forEach {
            mutatedGenes?.addRemovedOrAddedByAction(
                it,
                ind.seeActions(NO_INIT).indexOf(it),
                false,
                resourcePosition = pos
            )
        }
    }

    /**
     *  modify one of resource call with other template
     */
    private fun handleModify(ind: RestIndividual, mutatedGenes: MutatedGeneSpecification?){
        val auth = ind.seeActions().map { it.auth }.run {
            if (isEmpty()) null
            else randomness.choose(this)
        }

        val pos = randomness.choose(ind.getResourceCalls().filter { it.isDeletable }.map { ind.getResourceCalls().indexOf(it) })

        val old = ind.getResourceCalls()[pos]
        var max = config.maxTestSize
        ind.getResourceCalls().forEach { max -= it.seeActionSize(NO_SQL)}
        max += ind.getResourceCalls()[pos].seeActionSize(NO_SQL)
        var new = old.getResourceNode().generateAnother(old, randomness, max)
        if(new == null){
            new = old.getResourceNode().sampleOneAction(null, randomness)
        }
        maintainAuth(auth, new)

        //record removed
        ind.getResourceCalls()[pos].seeActions(ALL).forEach {
            mutatedGenes?.addRemovedOrAddedByAction(
                it,
                ind.seeActions(NO_INIT).indexOf(it),
                true,
                resourcePosition = pos
            )
        }

        ind.replaceResourceCall(pos, new)

        //record replaced
        new.seeActions(ALL).forEach {
            mutatedGenes?.addRemovedOrAddedByAction(
                it,
                ind.seeActions(NO_INIT).indexOf(it),
                false,
                resourcePosition = pos
            )
        }
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
            mutated.seeActions(NO_SQL).forEach { if(it is RestCallAction) it.auth = auth }
        }
    }

}