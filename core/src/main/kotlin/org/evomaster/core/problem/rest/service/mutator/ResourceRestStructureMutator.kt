package org.evomaster.core.problem.rest.service.mutator

import com.google.inject.Inject
import org.evomaster.core.Lazy
import org.evomaster.core.sql.SqlAction
import org.evomaster.core.sql.SqlInsertBuilder
import org.evomaster.core.problem.api.service.ApiWsStructureMutator
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestIndividual
import org.evomaster.core.problem.httpws.auth.HttpWsAuthenticationInfo
import org.evomaster.core.problem.rest.resource.ResourceImpactOfIndividual
import org.evomaster.core.problem.rest.resource.RestResourceCalls
import org.evomaster.core.problem.rest.service.ResourceDepManageService
import org.evomaster.core.problem.rest.service.ResourceManageService
import org.evomaster.core.problem.rest.service.sampler.ResourceSampler
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Individual
import org.evomaster.core.search.action.ActionFilter.*
import org.evomaster.core.search.impact.impactinfocollection.value.numeric.IntegerGeneImpact
import org.evomaster.core.search.service.mutator.MutatedGeneSpecification
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.ArchiveImpactSelector
import kotlin.math.max
import kotlin.math.min
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.math.roundToInt

class ResourceRestStructureMutator : ApiWsStructureMutator() {

    @Inject
    private lateinit var rm : ResourceManageService

    @Inject
    private lateinit var dm : ResourceDepManageService

    @Inject
    private lateinit var sampler : ResourceSampler

    @Inject
    private lateinit var mwc : MutationWeightControl

    @Inject
    private lateinit var archiveImpactSelector : ArchiveImpactSelector

    companion object{
        private val log : Logger = LoggerFactory.getLogger(ResourceRestStructureMutator::class.java)
    }

    override fun mutateStructure(individual: Individual, evaluatedIndividual: EvaluatedIndividual<*>, mutatedGenes: MutatedGeneSpecification?, targets: Set<Int>) {
        if(individual !is RestIndividual && evaluatedIndividual.individual is RestIndividual)
            throw IllegalArgumentException("Invalid individual type")

        individual as RestIndividual
        evaluatedIndividual as EvaluatedIndividual<RestIndividual>

        val dohandleSize =  config.isEnabledResourceSizeHandling() && (getAvailableMutator(individual).isEmpty() || randomness.nextBoolean(config.probOfHandlingLength))

        val mutationType = if (dohandleSize)
            decideMutationType(evaluatedIndividual)
        else null

        mutateRestResourceCalls(individual, mutationType, mutatedGenes = mutatedGenes, evaluatedIndividual, targets, dohandleSize)
        if (config.trackingEnabled()) tag(individual, time.evaluatedIndividuals)
    }

    private fun decideMutationType(evaluatedIndividual: EvaluatedIndividual<RestIndividual>) : MutationType?{
        // with adaptive structure mutator selection, we enable adding multiple same resource with rest action
        val candidates = getAvailableMutator(evaluatedIndividual.individual, true)
        // during focused search, we only involve the mutator which could change the size of resources
        val lengthMutator = if (candidates.size > 1) candidates.filterNot {
            // relatively low probability (i.e., 0.2) of applying rest actions to manipulate length
            it == MutationType.ADD && randomness.nextBoolean(0.8)
        } else candidates

        if (lengthMutator.isEmpty())
            return null

        return randomness.choose(lengthMutator)
    }

    fun mutateRestResourceCalls(ind: RestIndividual,
                                specified : MutationType?=null,
                                mutatedGenes: MutatedGeneSpecification? = null,
                                evaluatedIndividual: EvaluatedIndividual<RestIndividual>?=null, targets: Set<Int>?=null, dohandleSize: Boolean=false) {

        val executedStructureMutator = specified?: randomness.choose(getAvailableMutator(ind))

        when(executedStructureMutator){
            MutationType.ADD -> handleAdd(ind, mutatedGenes, evaluatedIndividual, targets, dohandleSize)
            MutationType.DELETE -> handleDelete(ind, mutatedGenes, evaluatedIndividual, targets, dohandleSize)
            MutationType.SWAP -> handleSwap(ind, mutatedGenes)
            MutationType.REPLACE -> handleReplace(ind, mutatedGenes)
            MutationType.MODIFY -> handleModify(ind, mutatedGenes)
        }
    }

    private fun getAvailableMutator(ind: RestIndividual, handleSize: Boolean = false) : List<MutationType>{
        val num = ind.getResourceCalls().size
        val sqlNum = ind.seeResource(RestIndividual.ResourceFilter.ONLY_SQL_INSERTION).size
        return MutationType.values()
            .filter {  num >= it.minSize && sqlNum >= it.minSQLSize && isMutationTypeApplicable(it, ind, handleSize)}

    }

    private fun isMutationTypeApplicable(type: MutationType, ind : RestIndividual, handleSize: Boolean): Boolean{
        val delSize = ind.getResourceCalls().filter(RestResourceCalls::isDeletable).size
        return when(type){
            MutationType.SWAP -> ind.extractSwapCandidates().isNotEmpty() && (!handleSize)
            MutationType.REPLACE -> !rm.cluster.doesCoverAll(ind) && delSize > 0 && (!handleSize)
            MutationType.MODIFY -> delSize > 0 && (!handleSize)
            MutationType.ADD -> ind.seeMainExecutableActions().size < config.maxTestSize
                    && (!rm.cluster.doesCoverAll(ind) || handleSize) && (config.maxResourceSize == 0 || (ind.getResourceCalls().size < config.maxResourceSize) )
            MutationType.DELETE -> delSize > 0 && ind.getResourceCalls().size >=2
        }
    }

    /**
     * the class defines possible methods to mutate ResourceRestIndividual regarding its resources
     * @param minSize is a minimum number of rest actions in order to apply the mutation
     * @param minSQLSize is a minimum number of db actions in order to apply the mutation
     */
    enum class MutationType(val minSize: Int, val minSQLSize : Int = 0){
        /**
         * remove a resource
         */
        DELETE(2),

        /**
         * swap two resources
         */
        SWAP(2),

        /**
         * add a new resource
         */
        ADD(1),

        /**
         * replace current resource with another one
         */
        REPLACE(1),

        /**
         * change a resource with different resource template
         */
        MODIFY(1)
    }

    override fun mutateInitStructure(individual: Individual, evaluatedIndividual: EvaluatedIndividual<*>, mutatedGenes: MutatedGeneSpecification?, targets: Set<Int>) {
        if (!config.isUsingAdvancedTechniques())
            throw IllegalStateException("resource-based solution currently is only enabled for MIO algorithm, but the algorithm is ${config.algorithm}")

        if (!randomness.nextBoolean(config.probOfSmartInitStructureMutator)){
            super.mutateInitStructure(individual, evaluatedIndividual, mutatedGenes, targets)
            return
        }

        if(individual !is RestIndividual && evaluatedIndividual.individual is RestIndividual)
            throw IllegalArgumentException("Invalid individual type")

        individual as RestIndividual
        evaluatedIndividual as EvaluatedIndividual<RestIndividual>

        if (randomness.nextBoolean()){
            handleAddSQL(individual, mutatedGenes, evaluatedIndividual, targets)
        }else{
            handleRemoveSQL(individual, mutatedGenes, evaluatedIndividual, targets)
        }
    }


    /**
     * add resources with SQL to [ind]
     * a number of resources to be added is related to EMConfig.maxSqlInitActionsPerResource
     */
    private fun handleAddSQL(ind: RestIndividual, mutatedGenes: MutatedGeneSpecification?, evaluatedIndividual: EvaluatedIndividual<RestIndividual>?, targets: Set<Int>?){

        val numOfResource = randomness.nextInt(1, max(1, getMaxSizeOfMutatingInitAction()))
        val candidates = if (doesApplyDependencyHeuristics())
            dm.identifyRelatedSQL(ind)
        else
            ind.seeInitializingActions().filterIsInstance<SqlAction>().map { it.table.name }.toSet() // adding an unrelated table would waste budget, then we add existing ones

        val selectedAdded = if (config.enableAdaptiveResourceStructureMutation){
            adaptiveSelectResource(evaluatedIndividual, bySQL = true, candidates.toList(), targets)
        }else{
            randomness.choose(candidates)
        }
        val added = createInsertSqlAction(selectedAdded, numOfResource)

        handleInitSqlAddition(ind, added, mutatedGenes)
    }


    private fun adaptiveSelectResource(evaluatedIndividual: EvaluatedIndividual<RestIndividual>?, bySQL: Boolean, candidates: List<String>, targets: Set<Int>?): String{
        evaluatedIndividual?: throw IllegalStateException("lack of impact with specified evaluated individual")
        targets?:throw IllegalStateException("targets must be specified if adaptive resource selection is applied")
        if (evaluatedIndividual.impactInfo == null || evaluatedIndividual.impactInfo !is ResourceImpactOfIndividual)
            throw IllegalStateException("lack of impact info or mismatched impact type (type: ${evaluatedIndividual.impactInfo?.javaClass?.simpleName?:"null"})")
        val impacts = candidates.map {
            if (bySQL){
                evaluatedIndividual.impactInfo.sqlTableSizeImpact[it] ?:IntegerGeneImpact("size")
            }else{
                evaluatedIndividual.impactInfo.resourceSizeImpact[it] ?:IntegerGeneImpact("size")
            }
        }

        val weights = archiveImpactSelector.impactBasedOnWeights(impacts, targets)
        val impactMap = candidates.mapIndexed { index, type -> type to weights[index] }.toMap()
        val selected = mwc.selectSubsetWithWeight(impactMap, true, 1.0)
        return if (selected.size == 1) selected.first() else randomness.choose(selected)
    }

    /**
     * remove one resource which are created by SQL
     *
     * Man: shall we remove SQLs which represents existing data?
     * It might be useful to reduce the useless db genes.
     */
    private fun handleRemoveSQL(ind: RestIndividual, mutatedGenes: MutatedGeneSpecification?, evaluatedIndividual: EvaluatedIndividual<RestIndividual>?, targets: Set<Int>?){
        val availableToRemove = ind.seeInitializingActions().filterIsInstance<SqlAction>().filterNot { it.representExistingData }

        // remove unrelated tables
        val candidates = if (doesApplyDependencyHeuristics())
            dm.identifyUnRelatedSqlTable(ind, availableToRemove)
        else availableToRemove.map { it.table.name }

        val selected = if (config.enableAdaptiveResourceStructureMutation)
            adaptiveSelectResource(evaluatedIndividual, true, candidates.toList(), targets)
        else randomness.choose(candidates)

        val total = candidates.count { it == selected }
        val num = randomness.nextInt(1, max(1, min(getMaxSizeOfMutatingInitAction(), min(total, availableToRemove.size - 1))))
        val remove = randomness.choose(availableToRemove.filter { it.table.name == selected }, num)
        handleInitSqlRemoval(ind, remove, mutatedGenes)
    }

    private fun doesApplyDependencyHeuristics() : Boolean{
        return dm.isDependencyNotEmpty()
                && randomness.nextBoolean(config.probOfEnablingResourceDependencyHeuristics)
    }

    /**
     * delete one or more resource call
     */
    private fun handleDelete(ind: RestIndividual, mutatedGenes: MutatedGeneSpecification?, evaluatedIndividual: EvaluatedIndividual<RestIndividual>?, targets: Set<Int>?, dohandleSize: Boolean){

        val candidates = if (doesApplyDependencyHeuristics()) dm.identifyDelNonDepResource(ind) else ind.getResourceCalls().filter(RestResourceCalls::isDeletable)

        val removedRes = if (config.enableAdaptiveResourceStructureMutation){
            adaptiveSelectResource(evaluatedIndividual, bySQL = false, candidates.map { it.getResourceKey() }.toSet().toList(), targets)
        }else{
            randomness.choose(candidates).getResourceKey()
        }

        val removedCandidates = candidates.filter { it.getResourceKey() == removedRes }
        val num = if (!dohandleSize) 1 else randomness.nextInt(1, max(1, min(rm.getMaxNumOfResourceSizeHandling(), min(removedCandidates.size, ind.getResourceCalls().size - 1))))
        val removes = randomness.choose(removedCandidates, num)

        removes.forEach { removed->
            val pos = ind.getResourceCalls().indexOf(removed)

            val removedActions = ind.getResourceCalls()[pos].seeActions(ALL)
            removedActions.forEach {
                mutatedGenes?.addRemovedOrAddedByAction(
                        it,
                        ind.seeFixedMainActions().indexOf(it),
                        localId = it.getLocalId(),
                        true,
                        resourcePosition = pos
                )
            }
        }
        ind.removeResourceCall(removes)
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
                mutatedGenes?.swapAction(pair.first, ind.getFixedActionIndexes(pair.first), ind.getFixedActionIndexes(pair.second))
                // skip to record dynamic action for swap mutation
                ind.swapResourceCall(pair.first, pair.second)
                return
            }
        }

        val randPair = randomizeSwapCandidates(candidates)
        val chosen = randPair.first
        val moveTo = randPair.second
        mutatedGenes?.swapAction(moveTo, ind.getFixedActionIndexes(chosen), ind.getFixedActionIndexes(moveTo))
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
    private fun handleAdd(ind: RestIndividual, mutatedGenes: MutatedGeneSpecification?, evaluatedIndividual: EvaluatedIndividual<RestIndividual>?, targets: Set<Int>?, dohandleSize: Boolean){

        val actions = ind.seeMainExecutableActions() as List<RestCallAction>

        val auth = actions.map { it.auth }.run {
            if (isEmpty()) null
            else randomness.choose(this)
        }

        val sizeOfCalls = ind.getResourceCalls().size

        var max = config.maxTestSize
        ind.getResourceCalls().forEach { max -= it.seeActions(MAIN_EXECUTABLE).size }
        if (max == 0){
            handleDelete(ind, mutatedGenes, evaluatedIndividual, targets, dohandleSize)
            return
        }


        if (dohandleSize){
            // only add existing resource, and there is no need to bind handling between resources
            val candnodes = ind.getResourceCalls().filter { it.node?.getTemplates()?.keys?.contains("POST") == true }.map { it.getResourceKey() }.toSet()
            if (candnodes.isNotEmpty()){
                val maxAddtionalRes =
                    if (config.maxResourceSize == 0)
                        rm.getMaxNumOfResourceSizeHandling()
                    else
                        min(rm.getMaxNumOfResourceSizeHandling(),  config.maxResourceSize - ind.getResourceCalls().size)

                val selected = if (config.enableAdaptiveResourceStructureMutation)
                    adaptiveSelectResource(evaluatedIndividual, bySQL = false, candnodes.toList(), targets)
                else randomness.choose(candnodes)

                val node = rm.getResourceNodeFromCluster(selected)
                val calls = node.sampleRestResourceCalls("POST", randomness, max)
                val num =  randomness.nextInt(1, max(1, min(maxAddtionalRes, (max*1.0/calls.seeActionSize(MAIN_EXECUTABLE)).roundToInt())))
                (0 until num).forEach { pos->
                    if (max > 0){
                        val added = if (pos == 0) calls else node.sampleRestResourceCalls("POST", randomness, max)
                        maintainAuth(auth, added)
                        ind.addResourceCall( pos, added)

                        Lazy.assert {
                            added.hasLocalId()
                        }

                        added.apply {
                            seeActions(ALL).forEach {
                                mutatedGenes?.addRemovedOrAddedByAction(
                                        it,
                                        ind.seeFixedMainActions().indexOf(it),
                                        localId = added.getLocalId(),
                                        false,
                                        resourcePosition = pos
                                )
                            }
                        }
                        max -= added.seeActionSize(MAIN_EXECUTABLE)
                    }
                }
                return
            }
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

            Lazy.assert {
                randomCall.hasLocalId()
            }
            randomCall.seeActions(ALL).forEach {
                mutatedGenes?.addRemovedOrAddedByAction(
                    it,
                    ind.seeFixedMainActions().indexOf(it),
                    localId = randomCall.getLocalId(),
                    false,
                    resourcePosition = pos
                )
            }

        }else{
            var addPos : Int? = null
            if(pair.first != null){
                val pos = ind.getResourceCalls().indexOf(pair.first!!)
                pair.first!!.bindWithOtherRestResourceCalls(mutableListOf(pair.second), rm.cluster,true, randomness = randomness)
                addPos = randomness.nextInt(0, pos)
            }
            if (addPos == null) addPos = randomness.nextInt(0, ind.getResourceCalls().size)

            maintainAuth(auth, pair.second)
            ind.addResourceCall( addPos, pair.second)

            pair.second.apply {
                seeActions(ALL).forEach {
                    mutatedGenes?.addRemovedOrAddedByAction(
                        it,
                        ind.seeFixedMainActions().indexOf(it),
                        localId = it.getLocalId(),
                        false,
                        resourcePosition = addPos
                    )
                }
            }
        }

        Lazy.assert { sizeOfCalls == ind.getResourceCalls().size - 1 }
    }

    /**
     * replace one of resource call with other resource
     */
    private fun handleReplace(ind: RestIndividual, mutatedGenes: MutatedGeneSpecification?){

        val actions = ind.seeMainExecutableActions() as List<RestCallAction>

        val auth = actions.map { it.auth }.run {
            if (isEmpty()) null
            else randomness.choose(this)
        }

        var max = config.maxTestSize
        ind.getResourceCalls().forEach { max -= it.seeActionSize(NO_SQL) }

        val fromDependency = doesApplyDependencyHeuristics()

        val pos = if(fromDependency){
            dm.handleDelNonDepResource(ind).run {
                ind.getIndexedResourceCalls().entries.first { it.value == this }!!.key
            }
        }else{
            randomness.choose(ind.getIndexedResourceCalls().entries
                    .filter { it.value.isDeletable }
                    .map { it.key })
        }


        max += ind.getIndexedResourceCalls()[pos]!!.seeActionSize(NO_SQL)

        val pair = if(fromDependency && pos != ind.getResourceCalls().size -1){
                        dm.handleAddDepResource(ind, max,
                                if (pos == ind.getViewOfChildren().size-1) mutableListOf()
                                else ind.getIndexedResourceCalls().filter { it.key > pos }
                                        .map { it.value }
                                        .toMutableList())
                    }else null

        var call = pair?.second
        if(pair == null){
            call =  rm.handleAddResource(ind, max)
        }else{
            if(pair.first != null){
                pair.first!!.bindWithOtherRestResourceCalls(mutableListOf(pair.second), rm.cluster,true, randomness = randomness)
            }
        }

       ind.getIndexedResourceCalls()[pos]!!.seeActions(ALL).forEach {
           if (ind.seeFixedMainActions().contains(it)){

               mutatedGenes?.addRemovedOrAddedByAction(
                   it,
                   ind.seeFixedMainActions().indexOf(it),
                   localId = it.getLocalId(),
                   true,
                   resourcePosition = pos
               )
           }else{
               mutatedGenes?.addRemovedOrAddedByAction(
                   it,
                   null,
                   localId = it.getLocalId(),
                   true,
                   resourcePosition = pos
               )
           }

       }

        ind.removeResourceCall(pos)

        maintainAuth(auth, call!!)
        ind.addResourceCall(pos, call)

        call.seeActions(ALL).forEach {
            if (ind.seeFixedMainActions().contains(it)){
                mutatedGenes?.addRemovedOrAddedByAction(
                    it,
                    ind.seeFixedMainActions().indexOf(it),
                    localId = it.getLocalId(),
                    false,
                    resourcePosition = pos
                )
            }else{
                mutatedGenes?.addRemovedOrAddedByAction(
                    it,
                    null,
                    localId = it.getLocalId(),
                    false,
                    resourcePosition = pos
                )
            }
        }
    }

    /**
     *  modify one of resource call with other template
     */
    private fun handleModify(ind: RestIndividual, mutatedGenes: MutatedGeneSpecification?){

        val actions = ind.seeMainExecutableActions() as List<RestCallAction>

        val auth = actions.map { it.auth }.run {
            if (isEmpty()) null
            else randomness.choose(this)
        }

        val pos = randomness.choose(
                ind.getIndexedResourceCalls().filter { it.value.isDeletable }
                        .map { it.key })

        val old = ind.getIndexedResourceCalls()[pos]!!
        var max = config.maxTestSize
        ind.getResourceCalls().forEach { max -= it.seeActionSize(MAIN_EXECUTABLE)}
        max += old.seeActionSize(MAIN_EXECUTABLE)
        var new = old.getResourceNode().generateAnother(old, randomness, max)
        if(new == null){
            new = old.getResourceNode().sampleOneAction(null, randomness)
        }
        maintainAuth(auth, new)

        //record removed
        old.seeActions(ALL).forEach {
            if (ind.seeFixedMainActions().contains(it)){
                mutatedGenes?.addRemovedOrAddedByAction(
                    it,
                    ind.seeFixedMainActions().indexOf(it),
                    localId = it.getLocalId(),
                    true,
                    resourcePosition = pos
                )
            }else{
                mutatedGenes?.addRemovedOrAddedByAction(
                    it,
                    null,
                    localId = it.getLocalId(),
                    true,
                    resourcePosition = pos
                )
            }
        }

        ind.replaceResourceCall(pos, new)

        //record replaced
        new.seeActions(ALL).forEach {
            if (ind.seeFixedMainActions().contains(it)){
                mutatedGenes?.addRemovedOrAddedByAction(
                    it,
                    ind.seeFixedMainActions().indexOf(it),
                    localId = it.getLocalId(),
                    false,
                    resourcePosition = pos
                )
            }else{
                mutatedGenes?.addRemovedOrAddedByAction(
                    it,
                    null,
                    localId = it.getLocalId(),
                    false,
                    resourcePosition = pos
                )
            }
        }
    }

    override fun addInitializingActions(individual: EvaluatedIndividual<*>, mutatedGenes: MutatedGeneSpecification?) {
        addInitializingActions(individual, mutatedGenes, sampler)
    }

    private fun maintainAuth(authInfo: HttpWsAuthenticationInfo?, mutated: RestResourceCalls){
        authInfo?.let { auth->
            mutated.seeActions(NO_SQL).forEach { if(it is RestCallAction) it.auth = auth }
        }
    }

    override fun getSqlInsertBuilder(): SqlInsertBuilder? {
        return sampler.sqlInsertBuilder
    }

    override fun canApplyActionStructureMutator(individual: Individual): Boolean {
        individual as RestIndividual
        return super.canApplyActionStructureMutator(individual)  &&
                (getAvailableMutator(individual, config.isEnabledResourceSizeHandling()).isNotEmpty() &&
                        ((!dm.onlyIndependentResource())  &&  // if all resources are asserted independent, there is no point to do structure mutation
                                dm.canMutateResource(individual))
                        )
    }

}