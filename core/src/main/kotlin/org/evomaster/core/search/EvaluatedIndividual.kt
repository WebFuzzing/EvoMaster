package org.evomaster.core.search

import org.evomaster.core.EMConfig
import org.evomaster.core.search.gene.*
import org.evomaster.core.search.impact.impactinfocollection.*
import org.evomaster.core.search.service.mutator.MutatedGeneSpecification
import org.evomaster.core.search.tracer.TraceableElement
import org.evomaster.core.search.tracer.TraceableElementCopyFilter
import org.evomaster.core.search.tracer.TrackOperator
import org.evomaster.core.Lazy
import org.evomaster.core.database.DbAction
import org.evomaster.core.problem.rest.RestIndividual
import org.evomaster.core.search.service.mutator.EvaluatedMutation
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.math.min

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
                             val results: List<out ActionResult>,

                             // for tracking its history
                             trackOperator: TrackOperator? = null,
                             index: Int = -1,
                             val impactInfo : ImpactsOfIndividual ?= null

) : TraceableElement(trackOperator, index) where T : Individual {

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
        if(individual.seeActions().size < results.size){
            throw IllegalArgumentException("Less actions than results")
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
     * Note: if a test execution was prematurely stopped,
     * the number of evaluated actions would be lower than
     * the total number of actions
     */
    fun evaluatedActions() : List<EvaluatedAction>{

        val list: MutableList<EvaluatedAction> = mutableListOf()

        val actions = individual.seeActions()

        (0 until results.size).forEach { i ->
            list.add(EvaluatedAction(actions[i], results[i]))
        }

        return list
    }

    /**
     * @return grouped db and evaluated actions based on its resource structure
     */
    fun evaluatedResourceActions() : List<Pair<List<DbAction>, List<EvaluatedAction>>>{
        if (individual !is RestIndividual)
            throw IllegalStateException("the method do not support the individual with the type: ${individual::class.java.simpleName}");

        val list = mutableListOf<Pair<List<DbAction>, List<EvaluatedAction>>>();

        var index = 0;

        individual.getResourceCalls().forEach { c->
            if (index < results.size){
                list.add(
                    c.dbActions to c.actions.subList(0, min(c.actions.size, results.size-index)).map {
                            a-> EvaluatedAction(a, results[index]).also { index++ }
                    }.toList()
                )
            }
        }
        Lazy.assert {
            index == results.size
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
                when {
                    copyFilter.name == ONLY_WITH_COPY_IMPACT -> return EvaluatedIndividual(
                            fitness.copy(),
                            individual.copy() as T,
                            results.map(ActionResult::copy),
                            trackOperator,
                            index,
                            impactInfo?.copy()
                    )
                    copyFilter.name == ONLY_WITH_CLONE_IMPACT-> return EvaluatedIndividual(
                            fitness.copy(),
                            individual.copy() as T,
                            results.map(ActionResult::copy),
                            trackOperator,
                            index,
                            impactInfo?.clone()
                    )
                    copyFilter.name == WITH_TRACK_WITH_COPY_IMPACT -> return EvaluatedIndividual(
                            fitness.copy(),
                            individual.copy() as T,
                            results.map(ActionResult::copy),
                            trackOperator,
                            index,
                            impactInfo?.copy()
                    ).also {
                        it.wrapWithTracking(evaluatedResult, trackingHistory = tracking)
                    }
                    copyFilter.name == WITH_TRACK_WITH_CLONE_IMPACT -> return EvaluatedIndividual(
                            fitness.copy(),
                            individual.copy() as T,
                            results.map(ActionResult::copy),
                            trackOperator,
                            index,
                            impactInfo?.clone()
                    ).also {
                        it.wrapWithTracking(evaluatedResult, trackingHistory = tracking)
                    }
                    else -> throw IllegalStateException("${copyFilter.name} is not available")
                }
            }
        }

    }

    fun nextForIndividual(next: TraceableElement, evaluatedResult: EvaluatedMutation): EvaluatedIndividual<T>? {
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

    override fun next(next: TraceableElement, copyFilter: TraceableElementCopyFilter, evaluatedResult: EvaluatedMutation): EvaluatedIndividual<T>? {

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

        Lazy.assert{mutatedGenes.mutatedIndividual != null}

        Lazy.assert {
            mutatedGenes.mutatedIndividual != null
            tracking != null
        }


        compareWithLatest(next = mutated, previous = previous, targetsInfo = targetsInfo, mutatedGenes = mutatedGenes)
    }

    private fun compareWithLatest(next : EvaluatedIndividual<T>, previous : EvaluatedIndividual<T>, targetsInfo: Map<Int, EvaluatedMutation>, mutatedGenes: MutatedGeneSpecification){

        val noImpactTargets = targetsInfo.filterValues { !it.isImpactful() }.keys
        val impactTargets = targetsInfo.filterValues {  it.isImpactful() }.keys
        val improvedTargets = targetsInfo.filterValues { it.isImproved() }.keys

        val didStructureMutation = mutatedGenes.didStructureMutation()
        if (didStructureMutation){ // structure mutated
            updateImpactsAfterStructureMutation(next, previous.individual, mutatedGenes, noImpactTargets, impactTargets, improvedTargets)
        }

        if ((!didStructureMutation)){
            impactInfo!!.syncBasedOnIndividual(individual, mutatedGenes)
        }

        if (didStructureMutation) return

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
        val sizeChanged = (mutatedGenes.mutatedIndividual!!.seeActions().size != previous.seeActions().size)

        //we update genes impact regarding structure only if structure mutated individual is 'next'
        if(this.index == next.index){
            if (mutatedGenes.removedGene.isNotEmpty()){ //delete an action
                impactInfo!!.deleteActionGeneImpacts(actionIndex = mutatedGenes.mutatedPosition.toSet())
            }else if (mutatedGenes.addedGenes.isNotEmpty()){ //add new action
                val groupGeneByActionIndex = mutatedGenes.addedGenes.groupBy {g->
                    mutatedGenes.mutatedIndividual!!.seeActions().find { a->a.seeGenes().contains(g) }.run { mutatedGenes.mutatedIndividual!!.seeActions().indexOf(this) }
                }

                groupGeneByActionIndex.toSortedMap().forEach { (actionIndex, mgenes) ->
                    if (!mutatedGenes.mutatedPosition.contains(actionIndex))
                        throw IllegalArgumentException("mismatched impact info")
                    impactInfo!!.addOrUpdateActionGeneImpacts(
                            actionIndex = actionIndex,
                            actionName = individual.seeActions()[actionIndex].getName(),
                            impacts = mgenes.map {g->
                                val id = ImpactUtils.generateGeneId(mutatedGenes.mutatedIndividual!!, g)
                                id to ImpactUtils.createGeneImpact(g,id)
                            }.toMap().toMutableMap(),
                            newAction = true
                    )
                }
            }else if (mutatedGenes.mutatedPosition.isNotEmpty()){
                Lazy.assert { mutatedGenes.mutatedPosition.toSet().size == 1 }
                val actionIndex = mutatedGenes.mutatedPosition.first()

                // add or remove an action which does not contain any genes
                if (individual.seeActions().size > previous.seeActions().size){
                    impactInfo!!.addOrUpdateActionGeneImpacts(
                            actionName = individual.seeActions()[actionIndex].getName(),
                            actionIndex = actionIndex,
                            newAction = true,
                            impacts = mutableMapOf()
                    )
                }else{
                    impactInfo!!.deleteActionGeneImpacts(actionIndex = setOf(actionIndex))
                }
            }
        }
        //TODO handle other kinds of mutation if it has e.g., replace, exchange
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
        mutated.seeActions().forEachIndexed { index, action ->
            action.seeGenes().filter { it.isMutable() }.forEach { sg->
                val rootGeneId = ImpactUtils.generateGeneId(mutated, sg)

                val p = previous.seeActions()[index].seeGenes().find { rootGeneId == ImpactUtils.generateGeneId(previous, it) }
                val impact = impactInfo!!.getGene(
                                actionName = action.getName(),
                                actionIndex = index,
                                fromInitialization = false,
                                geneId = rootGeneId
                        )?:throw IllegalArgumentException("fail to identify impact info for the gene $rootGeneId at $index of actions11")
                impact.syncImpact(p, sg)
            }
        }



    }

    fun findGeneById(id : String, index : Int = -1, isDb: Boolean=false) : Gene?{
        if (!isDb){
            if (index == -1) return individual.seeGenes().find { ImpactUtils.generateGeneId(individual, it) == id }
            if (index > individual.seeActions().size)
                throw IllegalArgumentException("index $index is out of boundary of actions ${individual.seeActions().size} of the individual")
            return individual.seeActions()[index].seeGenes().find { ImpactUtils.generateGeneId(individual, it) == id }
        }
        if (index == -1) return individual.seeInitializingActions().flatMap { it.seeGenes() }.find { ImpactUtils.generateGeneId(individual, it) == id }
        if (index >= individual.seeInitializingActions().size) return null
            //throw IllegalArgumentException("index $index is out of boundary of initializing actions ${individual.seeInitializingActions().size} of the individual")
        return individual.seeInitializingActions()[index].seeGenes().find { ImpactUtils.generateGeneId(individual, it) == id }
    }

    fun findGeneWithActionIndexAndGene(index: Int, gene: Gene, isInitializationAction : Boolean) : Gene?{
        val action = try {
            (if (isInitializationAction) individual.seeInitializingActions() else individual.seeActions()).elementAt(index)
        }catch(e: IndexOutOfBoundsException){
            return null
        }
        // gene should be one of root genes
        return ImpactUtils.findMutatedGene(action, gene)
    }


    //**************** for impact *******************//

    fun getImpactsRelatedTo(mutatedGenes: MutatedGeneSpecification) : List<Impact>{
        impactInfo?:return emptyList()

        if (mutatedGenes.didStructureMutation())
            return emptyList()
        val list = mutableListOf<Impact>()
        mutatedGenes.mutatedGenes.forEachIndexed { index, gene ->
            val actionIndex = mutatedGenes.mutatedPosition[index]
            val action = mutatedGenes.mutatedIndividual!!.seeActions()[actionIndex]
            val id = ImpactUtils.generateGeneId(action, gene.gene)
            val found = impactInfo.getGene(
                    actionName = action.getName(),
                    actionIndex = actionIndex,
                    geneId = id,
                    fromInitialization = false
            )?:throw IllegalArgumentException("mismatched impact info")
            list.add(found)
        }

        mutatedGenes.mutatedDbGenes.forEachIndexed { index, gene ->
            val actionIndex = mutatedGenes.mutatedDbActionPosition[index]
            val action = mutatedGenes.mutatedIndividual!!.seeInitializingActions()[actionIndex]
            val id = ImpactUtils.generateGeneId(action, gene.gene)
            val found = impactInfo.getGene(
                    actionName = action.getName(),
                    actionIndex = actionIndex,
                    geneId = id,
                    fromInitialization = true
            )?:throw IllegalArgumentException("mismatched impact info")
            list.add(found)
        }


        return list
    }

    fun anyImpactfulGene() : Boolean{
        impactInfo?:return false
        return impactInfo.anyImpactfulInfo()
    }

    /**
     *  gene impact can be added only if the gene is root gene
     *  this is to handle unclassified, eg, a gene might be empty gson or a gene constrained with some class
     */
    fun addGeneImpact(individual: Individual, gene: Gene) : GeneImpact?{
        val action = individual.seeActions().find {
            it.seeGenes().contains(gene)
        }
        if (action == null && !individual.seeGenes().contains(gene)) return null
        val index = individual.seeActions().indexOf(action)
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
        var action = individual.seeActions().find { it.seeGenes().contains(gene) }
        if (action != null){
            return impactInfo.getGene(
                    actionName = action.getName(),
                    actionIndex = individual.seeActions().indexOf(action),
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
                fromInitialization = individual.seeGenes(Individual.GeneFilter.ONLY_SQL).contains(gene)
        )
    }

    //TODO check this when integrating with SQL resource handling
    fun updateImpactGeneDueToAddedInitializationGenes(mutatedGenes: MutatedGeneSpecification, old : List<Action>, addedInsertions : List<List<Action>>?){
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
                actionName = if (fromInitialization) individual.seeInitializingActions()[actionIndex].getName() else individual.seeActions()[actionIndex].getName(),
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
}