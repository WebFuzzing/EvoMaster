package org.evomaster.core.search

import org.evomaster.core.EMConfig
import org.evomaster.core.search.gene.*
import org.evomaster.core.search.impact.impactinfocollection.*
import org.evomaster.core.search.service.mutator.MutatedGeneSpecification
import org.evomaster.core.search.tracer.Traceable
import org.evomaster.core.search.tracer.TraceableElementCopyFilter
import org.evomaster.core.search.tracer.TrackOperator
import org.evomaster.core.Lazy
import org.evomaster.core.database.DbAction
import org.evomaster.core.database.DbActionResult
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.problem.rest.RestCallResult
import org.evomaster.core.problem.rest.RestIndividual
import org.evomaster.core.search.Individual.GeneFilter
import org.evomaster.core.search.ActionFilter.*
import org.evomaster.core.search.service.mutator.EvaluatedMutation
import org.evomaster.core.search.tracer.TrackingHistory
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * EvaluatedIndividual allows to tracking its evolution.
 * Note that tracking EvaluatedIndividual can be enabled by set EMConfig.enableTrackEvaluatedIndividual true.
 */
class EvaluatedIndividual<T>(val fitness: FitnessValue,
                             val individual: T,
                             /**
                              * Note: as the test execution could had been
                              * prematurely stopped, there might be less
                              * results than actions
                              */
                             private val results: List<out ActionResult>,

                             // for tracking its history
                             override var trackOperator: TrackOperator? = null,
                             override var index: Int = Traceable.DEFAULT_INDEX,

                             // for impact
                             val impactInfo : ImpactsOfIndividual ?= null

) : Traceable where T : Individual {

    override var evaluatedResult: EvaluatedMutation? = null

    override var tracking: TrackingHistory<out Traceable>? = null

    companion object{
        const val ONLY_TRACKING_INDIVIDUAL_OF_EVALUATED = "ONLY_TRACKING_INDIVIDUAL_OF_EVALUATED"
        const val WITH_TRACK_WITH_CLONE_IMPACT = "WITH_TRACK_WITH_CLONE_IMPACT"
        const val WITH_TRACK_WITH_COPY_IMPACT = "WITH_TRACK_WITH_COPY_IMPACT"
        const val ONLY_WITH_COPY_IMPACT = "ONLY_WITH_COPY_IMPACT"
        const val ONLY_WITH_CLONE_IMPACT = "ONLY_WITH_CLONE_IMPACT"

        private val log: Logger = LoggerFactory.getLogger(EvaluatedIndividual::class.java)
    }

    /**
     * [hasImprovement] represents if [this] helps to improve Archive, e.g., reach new target.
     */
    var hasImprovement = false

    val clusterAssignments : MutableSet<String> = mutableSetOf()

    /**
     * How long it took to evaluate this individual.
     *
     * Note: could had it better to setup this value in the constructor, but it would had
     * led to quite a lot of refactoring. Furthermore, execution time is not fully deterministic,
     * and could make sense to re-update it when re-evaluating
     */
    var executionTimeMs : Long
        get() = fitness.executionTimeMs
        set(value) { fitness.executionTimeMs = value}

    init{
        if (individual.seeActions(ALL).size < results.size){
            throw IllegalArgumentException("Less actions (${individual.seeActions(ALL).size}) than results (${results.size})")
        }
    }

    constructor(fitness: FitnessValue, individual: T, results: List<out ActionResult>, trackOperator: TrackOperator? = null, index: Int = -1, config : EMConfig):
            this(fitness, individual, results,
                    trackOperator = trackOperator,
                    index = index,
                    impactInfo = if ((config.isEnabledImpactCollection()))
                        ImpactsOfIndividual(individual, config.abstractInitializationGeneToMutate, config.maxSqlInitActionsPerMissingData, fitness)
                    else
                        null)

    fun copy(): EvaluatedIndividual<T> {

        val ei = EvaluatedIndividual(
                fitness.copy(),
                individual.copy() as T,
                results.map(ActionResult::copy),
                trackOperator,
                index
        )
        ei.executionTimeMs = executionTimeMs
        ei.hasImprovement = hasImprovement
        ei.clusterAssignments.addAll(clusterAssignments)

        return ei
    }

    /**
     * @return action results based on the specified [actions].
     *      Note that if [actions] is null, then we employ individual.seeActions() as default
     */
    fun seeResults(actions: List<Action>? = null): List<ActionResult>{
        val list = actions?:individual.seeActions()
        val all = individual.seeActions(ALL)
        val last = results.indexOfFirst { it.stopping }
        return list.mapNotNull {
            val index = all.indexOf(it)
            if (last == -1 || index <= last)
                results[index]
            else null
        }
    }

    /**
     * Note: if a test execution was prematurely stopped,
     * the number of evaluated actions would be lower than
     * the total number of actions
     */
    fun evaluatedActions() : List<EvaluatedAction>{

        val list: MutableList<EvaluatedAction> = mutableListOf()

        val actions = individual.seeActions()
        val actionResults = seeResults(actions)

        (0 until actionResults.size).forEach { i ->
            list.add(EvaluatedAction(actions[i], actionResults[i]))
        }

        return list
    }

    /**
     * @return grouped evaluated actions based on its resource structure
     *      first are db actions and their results
     *      second are rest actions and their results
     */
    fun evaluatedResourceActions() : List<Pair<List<EvaluatedDbAction>, List<EvaluatedAction>>>{
        if (individual !is RestIndividual)
            throw IllegalStateException("the method do not support the individual with the type: ${individual::class.java.simpleName}");

        val list = mutableListOf<Pair<List<EvaluatedDbAction>, List<EvaluatedAction>>>();

        individual.getResourceCalls().forEach { c->
            val dbActions = c.seeActions(ONLY_SQL)
            val dbResults = seeResults(dbActions)

            val restActions = c.seeActions(NO_SQL)
            val restResult = seeResults(restActions)

            // get evaluated action based on the list of action results
            list.add(
                dbResults.mapIndexed { index, actionResult ->
                    EvaluatedDbAction(
                        (dbActions[index] as? DbAction) ?:throw IllegalStateException("mismatched action type, expected is DbAction but it is ${dbActions[index]::class.java.simpleName}"),
                        (actionResult as? DbActionResult)?:throw IllegalStateException("mismatched action result type, expected is DbActionResult but it is ${actionResult::class.java.simpleName}")
                    )
                } to restResult.mapIndexed { index, actionResult ->
                    EvaluatedAction(
                        (restActions[index] as? RestCallAction) ?:throw IllegalStateException("mismatched action type, expected is RestCallAction but it is ${restActions[index]::class.java.simpleName}"),
                        (actionResult as? RestCallResult)?:throw IllegalStateException("mismatched action result type, expected is RestCallResult but it is ${actionResult::class.java.simpleName}")
                    )
                }
            )
        }
        return list
    }

    override fun copy(copyFilter: TraceableElementCopyFilter): EvaluatedIndividual<T> {
        if(copyFilter.name == ONLY_TRACKING_INDIVIDUAL_OF_EVALUATED){
            return EvaluatedIndividual(
                    fitness.copy(),
                    individual.copy(TraceableElementCopyFilter.WITH_TRACK) as T,
                    results.map(ActionResult::copy),
                    trackOperator
            )
        }

        when(copyFilter){
            TraceableElementCopyFilter.NONE -> return copy()
            TraceableElementCopyFilter.WITH_ONLY_EVALUATED_RESULT ->{
                return copy().also {
                    it.wrapWithEvaluatedResults(evaluatedResult)
                }
            }
            TraceableElementCopyFilter.DEEP_TRACK -> throw IllegalArgumentException("there is no need to track individual when evaluated indivdual is tracked")
            TraceableElementCopyFilter.WITH_TRACK ->{
                // the copy includes tracking info, but it is no need to include tracking info for the element in the tracking.
                return copy().also { it.wrapWithTracking(evaluatedResult, trackingHistory = tracking) }
            }
            else -> {
                return EvaluatedIndividual(
                    fitness.copy(),
                    individual.copy() as T,
                    results.map(ActionResult::copy),
                    trackOperator,
                    index,
                    when (copyFilter.name) {
                        ONLY_WITH_COPY_IMPACT, WITH_TRACK_WITH_COPY_IMPACT -> {
                            impactInfo?.copy()
                        }
                        ONLY_WITH_CLONE_IMPACT, WITH_TRACK_WITH_CLONE_IMPACT -> {
                            impactInfo?.clone()
                        }
                        else -> throw IllegalStateException("${copyFilter.name} is not available")
                    }
                ).also {
                    if (copyFilter.name == WITH_TRACK_WITH_COPY_IMPACT
                        || copyFilter.name == WITH_TRACK_WITH_CLONE_IMPACT){
                        it.wrapWithTracking(evaluatedResult, trackingHistory = tracking)
                    }
                }
            }
        }

    }

    fun nextForIndividual(next: Traceable, evaluatedResult: EvaluatedMutation): EvaluatedIndividual<T>? {
        (next as? EvaluatedIndividual<T>) ?: throw IllegalArgumentException("mismatched tracking element")

        val nextIndividual = individual.next(next.individual, TraceableElementCopyFilter.WITH_TRACK, evaluatedResult)!!

        return EvaluatedIndividual(
                next.fitness.copy(),
                (nextIndividual as T).copy(TraceableElementCopyFilter.WITH_TRACK) as T,
                next.results.map(ActionResult::copy),
                next.trackOperator,
                next.index,
                impactInfo?.clone()
        )
    }

    override fun next(next: Traceable, copyFilter: TraceableElementCopyFilter, evaluatedResult: EvaluatedMutation): EvaluatedIndividual<T>? {

        tracking?: throw IllegalStateException("cannot create next due to unavailable tracking info")
        (next as? EvaluatedIndividual<T>) ?: throw IllegalArgumentException("mismatched tracking element")

        val nextInTracking = next.copy(copyFilter)
        nextInTracking.wrapWithEvaluatedResults(evaluatedResult)
        pushLatest(nextInTracking)

        val new = EvaluatedIndividual(
                next.fitness.copy(),
                next.individual.copy() as T,
                next.results.map(ActionResult::copy),
                next.trackOperator,
                next.index,
                impactInfo = impactInfo?.clone()
        )

        // tracking is shared with all mutated individual originated from same sampled ind
        new.wrapWithTracking(evaluatedResult, tracking)

        return new
    }


    /**
     * compare current with latest
     * [inTrack] indicates how to find the latest two elements to compare.
     * For instance, if the latest modification does not improve the fitness, it will be saved in [undoTracking].
     * in this case, the latest is the last of [undoTracking], not [this]
     */
    fun updateImpactOfGenes(previous : EvaluatedIndividual<T>, mutated : EvaluatedIndividual<T>, mutatedGenes: MutatedGeneSpecification, targetsInfo: Map<Int, EvaluatedMutation>){

        Lazy.assert {
            mutatedGenes.mutatedIndividual != null && tracking != null
        }

        if(previous.getSizeOfImpact(false) != mutated.getSizeOfImpact(false)){
            log.warn("impacts should be same before updating")
        }

        compareWithLatest(next = mutated, previous = previous, targetsInfo = targetsInfo, mutatedGenes = mutatedGenes)
    }

    private fun verifyImpacts(){
        impactInfo?.verifyActionGeneImpacts(individual.seeActions(NO_INIT))
    }

    private fun compareWithLatest(next : EvaluatedIndividual<T>, previous : EvaluatedIndividual<T>, targetsInfo: Map<Int, EvaluatedMutation>, mutatedGenes: MutatedGeneSpecification){

        val noImpactTargets = targetsInfo.filterValues { !it.isImpactful() }.keys
        val impactTargets = targetsInfo.filterValues {  it.isImpactful() }.keys
        val improvedTargets = targetsInfo.filterValues { it.isImproved() }.keys

        val didStructureMutation = mutatedGenes.didStructureMutation()
        if (didStructureMutation){ // structure mutated
            updateImpactsAfterStructureMutation(next, previous.individual, mutatedGenes, noImpactTargets, impactTargets, improvedTargets)
            verifyImpacts()
            return
        }

        if (!didStructureMutation){
            impactInfo!!.syncBasedOnIndividual(individual, mutatedGenes)
        }

        if (mutatedGenes.addedInitializationGenes.isNotEmpty()) {
            //TODO there is no any impact with added initialization, we may record this case.
            return
        }

        //we only sync impact info according to latest individual when next is this
        syncImpact(previous.individual, mutatedGenes.mutatedIndividual!!)

        updateImpactsAfterStandardMutation(previous = previous.individual, mutatedGenes = mutatedGenes, noImpactTargets = noImpactTargets, impactTargets = impactTargets, improvedTargets = improvedTargets)
    }
    private fun updateImpactsAfterStructureMutation(
            next: EvaluatedIndividual<T>,
            previous: Individual,
            mutatedGenes: MutatedGeneSpecification,
            noImpactTargets : Set<Int>,
            impactTargets: Set<Int>,
            improvedTargets: Set<Int>
    ){
        Lazy.assert { impactInfo != null }
        val sizeChanged = (mutatedGenes.mutatedIndividual!!.seeActions(NO_INIT).size != previous.seeActions(NO_INIT).size)

        //we update genes impact regarding structure only if structure mutated individual is 'next'
        if(this.index == next.index){

            //remove a number of resource with sql
            if (mutatedGenes.removedDbActions.isNotEmpty()){
                impactInfo!!.removeInitializationImpacts(mutatedGenes.removedDbActions, individual.seeInitializingActions().count { it is DbAction && it.representExistingData })
            }

            if (mutatedGenes.addedDbActions.isNotEmpty()){
                impactInfo!!.appendInitializationImpacts(mutatedGenes.addedDbActions)
            }

            //handle removed
            if (mutatedGenes.getRemoved(true).isNotEmpty()){ //delete an action
                impactInfo!!.deleteActionGeneImpacts(
                    actionIndex = mutatedGenes.getRemoved(true).mapNotNull { it.actionPosition }.toSet())
            }

            //handle added
            if (mutatedGenes.getAdded(true).isNotEmpty()){ //add new action
                val addedGenes = mutatedGenes.getAdded(true)
                //handle added actions with genes
                val groupGeneByActionIndex = addedGenes.filter { it.gene != null }.groupBy {g->
                    mutatedGenes.mutatedIndividual!!.seeActions(ActionFilter.NO_INIT).find {
                            a->a.seeGenes().contains(g.gene) }.run { mutatedGenes.mutatedIndividual!!.seeActions(ActionFilter.NO_INIT).indexOf(this) }
                }

                //handle added actions without genes
                val emptyActions = addedGenes.filter { it.gene == null }.mapNotNull { it.actionPosition }.toSet().sorted()

                addedGenes.mapNotNull { it.actionPosition }.toSet().sorted().forEach { actionIndex->
                      if (emptyActions.contains(actionIndex)){
                          impactInfo!!.addOrUpdateActionGeneImpacts(
                              actionName = individual.seeActions(ActionFilter.NO_INIT)[actionIndex].getName(),
                              actionIndex = actionIndex,
                              newAction = true,
                              impacts = mutableMapOf()
                          )
                      }else{
                          val mgenes = groupGeneByActionIndex.getValue(actionIndex)
                          val index = mgenes.mapNotNull { it.actionPosition }.toSet()
                          if (index.size != 1 || index.first() != actionIndex)
                              throw IllegalArgumentException("mismatched impact info: genes should be mutated at $index action, but actually the index is $actionIndex")
                          impactInfo!!.addOrUpdateActionGeneImpacts(
                              actionIndex = actionIndex,
                              actionName = individual.seeActions(ActionFilter.NO_INIT)[actionIndex].getName(),
                              impacts = mgenes.map {g->
                                  g.gene?:throw IllegalStateException("Added gene is not recorded")
                                  val id = ImpactUtils.generateGeneId(mutatedGenes.mutatedIndividual!!, g.gene)
                                  id to ImpactUtils.createGeneImpact(g.gene,id)
                              }.toMap().toMutableMap(),
                              newAction = true
                          )
                      }
                }
            }

            //handle swap
            if (mutatedGenes.getSwap().isNotEmpty()){
                if (mutatedGenes.getSwap().size > 1)
                    throw IllegalStateException("the swap mutator is applied more than one times, i.e., ${mutatedGenes.getSwap().size}")

                val swap = mutatedGenes.getSwap().first()
                val from = swap.from?:throw IllegalStateException("the resourcePosition is missing")
                val to = swap.to?:throw IllegalStateException("the swapToResourcePosition is missing")
                impactInfo!!.swapActionGeneImpact(from, to)
            }

            /*
                actions might be changed due to dependency handling or db repairing
                e.g., ind A is (table_a, table_b, resource_b)
                if added resource_b at the beginning, mutated ind A (table_a, resource_a, table_a, table_b, resource_b)
                in this case, we might remove second table_a, thus the mutated ind A becomes
                (table_a, resource_a, table_b, resource_b), and the table_b refers to the table_a in the front of resource_a
             */

            var fix = impactInfo!!.findFirstMismatchedIndex(individual.seeActions(ActionFilter.NO_INIT))
            while (fix.first != -1){
                if (fix.second!!){
                    impactInfo.deleteActionGeneImpacts(setOf(fix.first))
                }else{
                    impactInfo.addOrUpdateActionGeneImpacts(
                        actionName = individual.seeActions(ActionFilter.NO_INIT)[fix.first].getName(),
                        actionIndex = fix.first,
                        newAction = true,
                        impacts = mutableMapOf()
                    )
                }
                val nextFix = impactInfo.findFirstMismatchedIndex(individual.seeActions(ActionFilter.NO_INIT))
                if (nextFix.first < fix.first){
                    if (nextFix.first != -1)
                        log.warn("the fix at {} with remove/add ({}) does not work, and the next fix is at {}", fix.first, fix.second, nextFix.first)
                    break
                }
                fix = nextFix
            }
        }
        impactInfo!!.impactsOfStructure.countImpact(next, sizeChanged, noImpactTargets= noImpactTargets, impactTargets = impactTargets, improvedTargets = improvedTargets)

    }


    private fun updateImpactsAfterStandardMutation(
            previous: Individual,
            mutatedGenes: MutatedGeneSpecification,
            noImpactTargets : Set<Int>,
            impactTargets: Set<Int>,
            improvedTargets: Set<Int>
    ){

        if (mutatedGenes.didAddInitializationGenes()){
            Lazy.assert {
                impactInfo!!.getSQLExistingData() == individual.seeInitializingActions().count { it is DbAction && it.representExistingData }
            }
        }
        // update rest action genes and/or sql genes
        mutatedGenes.mutatedActionOrDb().forEach { db->
            val mutatedGenesWithContext = ImpactUtils.extractMutatedGeneWithContext(
                    mutatedGenes, mutatedGenes.mutatedIndividual!!, previousIndividual = previous, fromInitialization = db)

            mutatedGenesWithContext.forEach {gc->
                val impact = impactInfo!!.getGene(
                        actionName = gc.action,
                        actionIndex = gc.position,
                        geneId = ImpactUtils.generateGeneId(mutatedGenes.mutatedIndividual!!, gc.current),
                        fromInitialization = db
                )?:throw IllegalArgumentException("mismatched impact info")

                impact.countImpactWithMutatedGeneWithContext(
                        gc,
                        noImpactTargets = noImpactTargets,
                        impactTargets = impactTargets,
                        improvedTargets = improvedTargets,
                        onlyManipulation = false
                )
            }
        }
    }

    /**
     * sync impact info based on [mutated]
     */
    private fun syncImpact(previous : Individual, mutated : Individual) {

        // rest action
        mutated.seeActions(NO_INIT).forEachIndexed { index, action ->
            action.seeGenes().filter { it.isMutable() }.forEach { sg->
                val rootGeneId = ImpactUtils.generateGeneId(mutated, sg)

                val p = previous.seeActions(NO_INIT)[index].seeGenes().find {
                    rootGeneId == ImpactUtils.generateGeneId(previous, it)
                }
                val impact = impactInfo!!.getGene(
                                actionName = action.getName(),
                                actionIndex = index,
                                fromInitialization = false,
                                geneId = rootGeneId
                )?:throw IllegalArgumentException("fail to identify impact info for the gene $rootGeneId at $index of actions")
                impact.syncImpact(p, sg)
            }
        }
    }

    //**************** for impact *******************//


    fun anyImpactfulGene() : Boolean{
        impactInfo?:return false
        return impactInfo.anyImpactfulInfo()
    }

    /**
     *  gene impact can be added only if the gene is root gene
     *  this is to handle unclassified, eg, a gene might be empty gson or a gene constrained with some class
     */
    fun addGeneImpact(individual: Individual, gene: Gene) : GeneImpact?{
        val actions = if (individual is RestIndividual)  individual.seeActions(NO_INIT) else individual.seeActions()

        val action = actions.find {
            it.seeGenes().contains(gene)
        }
        if (action == null && !individual.seeGenes().contains(gene)) return null

        val index = actions.indexOf(action)

        val geneId = ImpactUtils.generateGeneId(individual, gene)
        val impact = ImpactUtils.createGeneImpact(gene,geneId)
        impactInfo?.addOrUpdateActionGeneImpacts(
                actionName = action?.getName(),
                actionIndex = index,
                newAction = false,
                impacts = mutableMapOf(geneId to ImpactUtils.createGeneImpact(gene,geneId))
        )
        return impact
    }

    fun getImpact(individual: Individual, gene: Gene) : GeneImpact?{
        impactInfo?:return null

        val id = ImpactUtils.generateGeneId(individual, gene)
        var action = individual.seeActions(NO_INIT).find { it.seeGenes().contains(gene) }
        if (action != null){
            return impactInfo.getGene(
                    actionName = action.getName(),
                    actionIndex = individual.seeActions(NO_INIT).indexOf(action),
                    geneId = id,
                    fromInitialization = false
            )
        }
        action = individual.seeInitializingActions().find { it.seeGenes().contains(gene) }
        if (action != null){
            return impactInfo.getGene(
                    actionName = action.getName(),
                    actionIndex = individual.seeInitializingActions().indexOf(action),
                    geneId = id,
                    fromInitialization = true
            )
        }

        return impactInfo.getGene(
                actionName = null,
                actionIndex = null,
                geneId = id,
                fromInitialization = individual.seeGenes(GeneFilter.ONLY_SQL).contains(gene)
        )
    }

    //TODO check this when integrating with SQL resource handling
    fun updateImpactGeneDueToAddedInitializationGenes(
        mutatedGenes: MutatedGeneSpecification,
        old : List<Action>,
        addedInsertions : List<List<Action>>?
    ){
        impactInfo?:throw IllegalStateException("there is no any impact initialized")

        val allExistingData = individual.seeInitializingActions().filter { it is DbAction && it.representExistingData }
        val diff = individual.seeInitializingActions().filter { !old.contains(it) && it is DbAction && !it.representExistingData}

        if (allExistingData.isNotEmpty())
            impactInfo.updateExistingSQLData(allExistingData.size)

        if (diff.isEmpty()) {
            return
        }
        mutatedGenes.addedInitializationGenes.addAll(diff.flatMap { it.seeGenes() })

        // update impact due to newly added initialization actions
        val modified =  if (addedInsertions!!.flatten().size == diff.size)
            addedInsertions
        else if (addedInsertions.flatten().size > diff.size){
            addedInsertions.mapNotNull {
                val m = it.filter { a-> diff.contains(a) }
                if (m.isEmpty()) null else m
            }
        }else{
            log.warn("unexpected handling on Initialization Action after repair")
            return
        }

        if(modified.flatten().size != diff.size){
            log.warn("unexpected handling on Initialization Action")
            return
        }

        if(old.isEmpty()){
            initAddedInitializationGenes(modified, allExistingData.size)
        }else{
            impactInfo.updateInitializationImpactsAtBeginning(modified, allExistingData.size)
        }

        mutatedGenes.addedInitializationGroup.addAll(modified)

        Lazy.assert {
            individual.seeInitializingActions().filter { it is DbAction && !it.representExistingData }.size == impactInfo.getSizeOfActionImpacts(true)
        }
    }


    fun initAddedInitializationGenes(group: List<List<Action>>, existingSize : Int){
        impactInfo!!.initInitializationImpacts(group, existingSize)
    }

    fun anyImpactInfo() : Boolean{
        impactInfo?:return false
        return impactInfo.anyImpactInfo()
    }

    fun flattenAllGeneImpact() : List<GeneImpact>{
        impactInfo?:return listOf()
        return impactInfo.flattenAllGeneImpact()
    }

    fun getSizeOfImpact(fromInitialization : Boolean) : Int{
        impactInfo?: return -1
        return impactInfo.getSizeOfActionImpacts(fromInitialization)
    }

    fun getImpactByAction(actionIndex : Int, fromInitialization: Boolean) : MutableMap<String, GeneImpact>?{
        impactInfo?:return null
        return impactInfo.findImpactsByAction(
                actionIndex = actionIndex,
                actionName = if (fromInitialization) individual.seeInitializingActions()[actionIndex].getName() else individual.seeActions(ActionFilter.NO_INIT)[actionIndex].getName(),
                fromInitialization = fromInitialization
        )
    }

    fun getInitializationGeneImpact(): List<MutableMap<String, GeneImpact>>{
        impactInfo?:throw IllegalArgumentException("impact info is null")
        return impactInfo.getInitializationGeneImpact()
    }

    fun getActionGeneImpact(): List<MutableMap<String, GeneImpact>>{
        impactInfo?:throw IllegalArgumentException("impact info is null")
        return  impactInfo.getActionGeneImpact()
    }

    fun getGeneImpact(geneId : String) : List<GeneImpact>{
        impactInfo?:return listOf()
        return impactInfo.getGeneImpact(geneId)
    }

    fun exportImpactAsListString(targets: Set<Int>? = null) : MutableList<String>{
        impactInfo?: throw IllegalArgumentException("do not enable collecting impacts info")

        val content = mutableListOf<String>()

        impactInfo.exportImpactInfo(false, content, targets)
        impactInfo.exportImpactInfo(true, content, targets)

        return content
    }

    fun assignToCluster(cluster : String) : EvaluatedIndividual<T>{
        clusterAssignments.add(cluster)
        return this
    }

    fun getClusters(): MutableSet<String> {
        return clusterAssignments
    }

    fun belongsToCluster(cluster: String) : Boolean{
        return clusterAssignments.contains(cluster)
    }

    fun isValid() : Boolean{
        val index = results.indexOfFirst { it.stopping }
        val all = individual.seeActions(ALL)
        if (results.size > all.size) return false
        val invalid = (0 until (if(index==-1) index+1 else results.size)).any {
            !results[it].matchedType(all[it])
        }
        return !invalid
    }
}