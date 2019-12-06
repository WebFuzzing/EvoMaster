package org.evomaster.core.search

import org.evomaster.core.EMConfig
import org.evomaster.core.search.gene.*
import org.evomaster.core.search.impact.*
import org.evomaster.core.search.service.mutator.MutatedGeneSpecification
import org.evomaster.core.search.tracer.TraceableElement
import org.evomaster.core.search.tracer.TraceableElementCopyFilter
import org.evomaster.core.search.tracer.TrackOperator
import org.evomaster.core.Lazy
import org.evomaster.core.database.DbAction

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
                             trackOperator: TrackOperator? = null,
                             tracking : MutableList<EvaluatedIndividual<T>>? = null,
                             undoTracking : MutableList<EvaluatedIndividual<T>>? = null,
                             private val impactInfo : ImpactsOfIndividual ?= null
) : TraceableElement(trackOperator,  tracking, undoTracking) where T : Individual {

    companion object{
        const val ONLY_INDIVIDUAL = "ONLY_INDIVIDUAL"
        const val WITH_TRACK_WITH_CLONE_IMPACT = "WITH_TRACK_WITH_CLONE_IMPACT"
        const val WITH_TRACK_WITH_COPY_IMPACT = "WITH_TRACK_WITH_COPY_IMPACT"
    }

    /**
     * [hasImprovement] represents if [this] helps to improve Archive, e.g., reach new target.
     */
    var hasImprovement = false

    var mutatedGeneSpecification : MutatedGeneSpecification? = null

    init{
        if(individual.seeActions().size < results.size){
            throw IllegalArgumentException("Less actions than results")
        }
    }

    constructor(fitness: FitnessValue, individual: T, results: List<out ActionResult>, enableTracking: Boolean, trackOperator: TrackOperator?, enableImpact: Boolean):
            this(fitness, individual, results,
                    trackOperator = trackOperator, tracking = if (enableTracking) mutableListOf() else null, undoTracking = if (enableTracking) mutableListOf() else null,
                    impactInfo = if (enableImpact) ImpactsOfIndividual() else null
            ){
        if(enableImpact) initImpacts()
    }

    fun copy(): EvaluatedIndividual<T> {
        return EvaluatedIndividual(
                fitness.copy(),
                individual.copy() as T,
                results.map(ActionResult::copy),
                trackOperator
        )
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

    override fun copy(copyFilter: TraceableElementCopyFilter): EvaluatedIndividual<T> {
        when(copyFilter){
            TraceableElementCopyFilter.NONE -> return copy()
            TraceableElementCopyFilter.WITH_TRACK ->{
                return EvaluatedIndividual(
                        fitness.copy(),
                        individual.copy() as T,
                        results.map(ActionResult::copy),
                        trackOperator?:individual.trackOperator,
                        getTracking()?.map { it.copy() }?.toMutableList()?: mutableListOf(),
                        getUndoTracking()?.map { it.copy()}?.toMutableList()?: mutableListOf()
                ).also { current->
                    mutatedGeneSpecification?.let {
                        current.mutatedGeneSpecification = it.copyFrom(current)
                    }
                }
            }

            TraceableElementCopyFilter.DEEP_TRACK -> {
                throw IllegalArgumentException("${copyFilter.name} should be not applied for EvaluatedIndividual")
            }
            else ->{
                when {
                    copyFilter.name == ONLY_INDIVIDUAL -> return EvaluatedIndividual(
                            fitness.copy(),
                            individual.copy(TraceableElementCopyFilter.WITH_TRACK) as T,
                            results.map(ActionResult::copy),
                            trackOperator
                    )
                    copyFilter.name == WITH_TRACK_WITH_CLONE_IMPACT -> {
                        return EvaluatedIndividual(
                                fitness.copy(),
                                individual.copy(TraceableElementCopyFilter.NONE) as T,
                                results.map(ActionResult::copy),
                                trackOperator?:individual.trackOperator,
                                getTracking()?.map { it.copy() }?.toMutableList()?: mutableListOf(),
                                getUndoTracking()?.map { it.copy()}?.toMutableList()?: mutableListOf(),
                                impactInfo = impactInfo!!.clone()
                        ).also { current->
                            mutatedGeneSpecification?.let {
                                current.mutatedGeneSpecification = it.copyFrom(current)
                            }
                        }
                    }
                    copyFilter.name == WITH_TRACK_WITH_COPY_IMPACT -> {
                        return EvaluatedIndividual(
                                fitness.copy(),
                                individual.copy(TraceableElementCopyFilter.NONE) as T,
                                results.map(ActionResult::copy),
                                trackOperator?:individual.trackOperator,
                                getTracking()?.map { it.copy() }?.toMutableList()?: mutableListOf(),
                                getUndoTracking()?.map { it.copy()}?.toMutableList()?: mutableListOf(),
                                impactInfo = impactInfo!!.copy()
                        ).also { current->
                            mutatedGeneSpecification?.let {
                                current.mutatedGeneSpecification = it.copyFrom(current)
                            }
                        }
                    }
                    else -> throw IllegalStateException("${copyFilter.name} is not implemented!")
                }
            }
        }
    }

    fun getHistoryOfGene(gene: Gene, geneId : String, length : Int = -1) : List<Gene>{
        /*
        TODO if gene is not root gene
         */
        getTracking()?: throw IllegalArgumentException("tracking is not enabled")
        return getTracking()!!.flatMap { it.individual.seeGenes().find { g->ImpactUtils.generateGeneId(it.individual, g) == geneId}.run {
            if (this == null || this::class.java.simpleName  != gene::class.java.simpleName) listOf() else listOf(this)
        } }
    }

    /**
     * get latest modification with respect to the [gene]
     */
    fun getLatestGene(gene: Gene) : Gene?{
        getTracking()?: throw IllegalArgumentException("tracking is not enabled")
        if (getTracking()!!.isEmpty()) return null
        val latestInd = getTracking()!!.last().individual

        val geneId = ImpactUtils.generateGeneId(individual, gene)

        //the individual was mutated in terms of structure, the gene might be found in history
        val latest = latestInd.seeGenes().find { ImpactUtils.generateGeneId(latestInd, it) == geneId }?:return null
        return if (latest::class.java.simpleName == gene::class.java.simpleName) latest else null
    }

    private fun getReachedTarget() : MutableMap<Int, Double>{
        if (impactInfo == null) throw IllegalStateException("this method should be invoked")
        return impactInfo.reachedTargets
    }

    fun updateUndoTracking(evaluatedIndividual: EvaluatedIndividual<T>, maxLength: Int){
        if (getUndoTracking()?.size?:0 == maxLength && maxLength > 0){
            getUndoTracking()?.removeAt(0)
        }
        getUndoTracking()?.add(evaluatedIndividual)
    }

    override fun next(trackOperator: TrackOperator, next: TraceableElement, copyFilter: TraceableElementCopyFilter, maxLength : Int): EvaluatedIndividual<T>? {
        if (next !is EvaluatedIndividual<*>) throw  IllegalArgumentException("the type of next is mismatched")

        when(copyFilter){
            TraceableElementCopyFilter.NONE, TraceableElementCopyFilter.DEEP_TRACK -> throw IllegalArgumentException("incorrect invocation")
            TraceableElementCopyFilter.WITH_TRACK -> {
                return EvaluatedIndividual(
                        next.fitness.copy(),
                        next.individual.copy() as T,
                        next.results.map(ActionResult::copy),
                        trackOperator,
                        tracking = (getTracking()?.plus(this)?.map { it.copy()}?.toMutableList()?: mutableListOf(this.copy())).run {
                            if (size == maxLength && maxLength > 0){
                                this.removeAt(0)
                                this
                            }else
                                this
                        },
                        undoTracking = (getUndoTracking()?.map { it.copy()}?.toMutableList()?: mutableListOf()).run {
                            if (size == maxLength && maxLength > 0){
                                this.removeAt(0)
                                this
                            }else
                                this
                        }
                ).also { current->
                    mutatedGeneSpecification?.let {
                        current.mutatedGeneSpecification = it.copyFrom(current)
                    }
                }
            }else ->{
            when {
                copyFilter.name == WITH_TRACK_WITH_CLONE_IMPACT -> return EvaluatedIndividual(
                        next.fitness.copy(),
                        next.individual.copy() as T,
                        next.results.map(ActionResult::copy),
                        trackOperator,
                        tracking = (getTracking()?.plus(this)?.map { it.copy()}?.toMutableList()?: mutableListOf(this.copy())).run {
                            if (size == maxLength && maxLength > 0){
                                this.removeAt(0)
                                this
                            }else
                                this
                        },
                        undoTracking = (getUndoTracking()?.map { it.copy()}?.toMutableList()?: mutableListOf()).run {
                            if (size == maxLength && maxLength > 0){
                                this.removeAt(0 )
                                this
                            }else
                                this
                        },
                        impactInfo = impactInfo!!.clone()
                ).also { current->
                    mutatedGeneSpecification?.let {
                        current.mutatedGeneSpecification = it.copyFrom(current)
                    }
                }
                copyFilter.name == WITH_TRACK_WITH_COPY_IMPACT -> return EvaluatedIndividual(
                        next.fitness.copy(),
                        next.individual.copy() as T,
                        next.results.map(ActionResult::copy),
                        trackOperator,
                        tracking = (getTracking()?.plus(this)?.map { it.copy()}?.toMutableList()?: mutableListOf(this.copy())).run {
                            if (size == maxLength && maxLength > 0){
                                this.removeAt(0)
                                this
                            }else
                                this
                        },
                        undoTracking = (getUndoTracking()?.map { it.copy()}?.toMutableList()?: mutableListOf()).run {
                            if (size == maxLength && maxLength > 0){
                                this.removeAt(0 )
                                this
                            }else
                                this
                        },
                        impactInfo = impactInfo!!.copy()
                ).also { current->
                    mutatedGeneSpecification?.let {
                        current.mutatedGeneSpecification = it.copyFrom(current)
                    }
                }
                copyFilter.name == ONLY_INDIVIDUAL -> IllegalArgumentException("incorrect invocation")
            }

                throw IllegalStateException("${copyFilter.name} is not implemented!")
            }
        }
    }

    override fun getUndoTracking(): MutableList<EvaluatedIndividual<T>>? {
        if(super.getUndoTracking() == null) return null
        return super.getUndoTracking() as MutableList<EvaluatedIndividual<T>>
    }


    /**
     * compare current with latest
     * [inTrack] indicates how to find the latest two elements to compare.
     * For instance, if the latest modification does not improve the fitness, it will be saved in [undoTracking].
     * in this case, the latest is the last of [undoTracking], not [this]
     */
    fun updateImpactOfGenes(
            inTrack : Boolean,
            mutatedGenes : MutatedGeneSpecification,
            impactTargets : MutableSet<Int>,
            improvedTargets: MutableSet<Int>){
        Lazy.assert{mutatedGenes.mutatedIndividual != null}
        Lazy.assert{getTracking() != null}
        Lazy.assert{getUndoTracking() != null}

        if(inTrack) Lazy.assert{getTracking()!!.isNotEmpty()}
        else Lazy.assert{getUndoTracking()!!.isNotEmpty()}

        val previous = if(inTrack) getTracking()!!.last() else this
        val next = if(inTrack) this else getUndoTracking()!!.last()

        updateReachedTargets(fitness)

        compareWithLatest(next, previous, improvedTargets, impactTargets, mutatedGenes)
    }



    private fun compareWithLatest(next : EvaluatedIndividual<T>, previous : EvaluatedIndividual<T>, improvedTargets : Set<Int>, impactTargets: Set<Int>, mutatedGenes: MutatedGeneSpecification){
        /*
          genes of actions of individual might be added with additionalInfoList
         */
        addGeneForActionIfMissing(next.individual, excludeGenes = mutatedGenes.allManipulatedGenes())//donot add action
        /*
          genes of initialization of individual might be added by fixing relationships among dbactions
         */
        updateDbGenesDueToFixing(next.individual)

        val noImpactTargets = next.fitness.getViewOfData().keys.filterNot { impactTargets.contains(it) }.toSet()

        if (mutatedGenes.mutatedGenes.isEmpty() && mutatedGenes.mutatedDbGenes.isEmpty()){ // structure mutated
            updateImpactsAfterStructureMutation(next, previous.individual, mutatedGenes, noImpactTargets, impactTargets, improvedTargets)
            return
        }
        if (mutatedGenes.addedInitializationGenes.isNotEmpty()) return

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
        val sizeChanged = (mutatedGenes.mutatedIndividual!!.seeActions().size != previous.seeActions().size)

        /*
        the structure change should be slight, one was removed or added for action
        NOTE THAT structure mutation does not include dbaction
         */
        if (mutatedGenes.removedGene.isNotEmpty()){ //delete an action
            impactInfo!!.deleteGeneImpacts(mutatedGenes.mutatedPosition.toSet(), forInit = false)
        }
        if (mutatedGenes.addedGenes.isNotEmpty()){ //add new action
            mutatedGenes.mutatedPosition.toSet().apply {
                if (size == 1){
                    val index = first()
                    impactInfo!!.addOrUpdateGeneImpacts(
                            index,
                            mutatedGenes.addedGenes.map {g->
                                val id = ImpactUtils.generateGeneId(mutatedGenes.mutatedIndividual!!, g)
                                id to ImpactUtils.createGeneImpact(g,id)
                            }.toMap().toMutableMap(),
                            forInit = false,
                            newLine = true
                    )
                }else{
                    //TODO
                }
            }
        }
        //TODO handle other kinds of mutation if it has e.g., replace, exchange
        impactInfo?.impactsOfStructure?.countImpact(next, sizeChanged, noImpactTargets= noImpactTargets, impactTargets = impactTargets, improvedTargets = improvedTargets)
    }
    private fun updateImpactsAfterStandardMutation(
            previous: Individual,
            mutatedGenes: MutatedGeneSpecification,
            noImpactTargets : Set<Int>,
            impactTargets: Set<Int>,
            improvedTargets: Set<Int>
    ){

        /*
        NOTE THAT if applying 1/n, a number of mutated genes may be more than 1 (e.g., n = 2).
        This might have side effects to impact analysis, so we only collect no impact info and ignore to collect impacts info.
        But times of manipulation should be updated.
         */
        val onlyManipulation = false//((mutatedGenes.mutatedGenes.size + mutatedGenes.mutatedDbGenes.size) > 1) && impactTargets.isNotEmpty()

        val mutatedGenesWithContext = ImpactUtils.extractMutatedGeneWithContext(
                mutatedGenes.mutatedGenes, mutatedGenes.mutatedIndividual!!, previousIndividual = previous)

        mutatedGenesWithContext.forEach { (t, u) ->

            u.forEach { gc ->
                val impact = impactInfo!!.getGene(
                        t,
                        gc.position,
                        isIndex4Init = false
                )
                (impact?:throw IllegalArgumentException("mismatched individual and previous individual")) as? GeneImpact?:throw IllegalArgumentException("mismatched gene type")
                (impact as GeneImpact).countImpactWithMutatedGeneWithContext(
                        gc,
                        noImpactTargets = noImpactTargets,
                        impactTargets = impactTargets,
                        improvedTargets = improvedTargets,
                        onlyManipulation = onlyManipulation
                )
            }
        }

        val mutatedDBGenesWithContext = ImpactUtils.extractMutatedDbGeneWithContext(
                mutatedGenes.mutatedDbGenes, mutatedGenes.mutatedIndividual!!, previousIndividual = previous)
        mutatedDBGenesWithContext.forEach { (t, u) ->
            u.forEach { gc ->
                val impact = impactInfo!!.getGene(
                        t,
                        gc.position,
                        isIndex4Init = true
                )
                (impact?:throw IllegalArgumentException("mismatched individual and previous individual")) as? GeneImpact?:throw IllegalArgumentException("mismatched gene type")
                (impact as GeneImpact).countImpactWithMutatedGeneWithContext(
                        gc,
                        noImpactTargets = noImpactTargets,
                        impactTargets = impactTargets,
                        improvedTargets = improvedTargets,
                        onlyManipulation = onlyManipulation)
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

    /**
     * update fitness archived by [this]
     * return whether [fitness] archive new targets or improve distance
     */
    private fun updateReachedTargets(fitness: FitnessValue) : List<Int>{
        val difference = mutableListOf<Int>()
        fitness.getViewOfData().forEach { (t, u) ->
            var previous = getReachedTarget()[t]
            if(previous == null){
                difference.add(t)
                previous = 0.0
                getReachedTarget()[t] = previous
            }else{
                if(u.distance > previous){
                    difference.add(t)
                    getReachedTarget()[t] = u.distance
                }
            }
        }
        return difference
    }

    override fun getTracking(): List<EvaluatedIndividual<T>>? {
        val tacking = super.getTracking()?:return null
        if(tacking.all { it is EvaluatedIndividual<*> })
            return tacking as List<EvaluatedIndividual<T>>
        else
            throw IllegalArgumentException("tracking has elements with mismatched type")
    }


    //**************** for impact *******************//

    fun existGeneId(geneId : String) : Boolean {
        impactInfo?:return false
        return impactInfo.exist(geneId)
    }

    fun getImpactfulTargets(mutatedGenes: MutatedGeneSpecification?) : Set<Int>{
        if (mutatedGenes == null){
            impactInfo?:return setOf()
            return impactInfo.getAllImpactfulTargets()
        }
        return getImpacts(mutatedGenes).flatMap { it.getTimesOfImpacts().keys }.toSet()
    }

    fun getActionGeneImpact() : MutableList<MutableMap<String, GeneImpact>>{
        impactInfo?: return mutableListOf()
        return impactInfo.actionGeneImpacts
    }
    fun getInitializationGeneImpact() : MutableList<MutableMap<String, GeneImpact>>{
        impactInfo?: return mutableListOf()
        return impactInfo.initializationGeneImpacts
    }
    fun getImpacts(mutatedGenes: MutatedGeneSpecification) : List<Impact>{
        impactInfo?:return emptyList()

        if (mutatedGenes.didStructureMutation())
            return emptyList()

        Lazy.assert {
            mutatedGenes.mutatedGenes.size == mutatedGenes.mutatedPosition.size
        }
        val impact = mutatedGenes.mutatedGenes.mapIndexed { index, gene ->
            val actionIndex = mutatedGenes.mutatedPosition[index]
            val id = ImpactUtils.generateGeneId(mutatedGenes.mutatedIndividual!!, gene)
            impactInfo.getGene(id, actionIndex, false)?:throw IllegalArgumentException("mismatched gene and its impact info during mutation")
        }

        Lazy.assert {
            mutatedGenes.mutatedDbGenes.size == mutatedGenes.mutatedDbActionPosition.size
        }
        impact.plus( mutatedGenes.mutatedDbGenes.mapIndexed { index, gene ->
            val actionIndex = mutatedGenes.mutatedDbActionPosition[index]
            val id = ImpactUtils.generateGeneId(mutatedGenes.mutatedIndividual!!, gene)
            impactInfo.getGene(id, actionIndex, false)?:throw IllegalArgumentException("mismatched gene and its impact info during mutation")
        })

        return impact
    }

    fun anyImpactfulGene() : Boolean{
        impactInfo?:return false
        return impactInfo.anyImpactfulInfo()
    }

    fun getImpact(individual: Individual, gene: Gene) : GeneImpact?{
        impactInfo?:return null

        val id = ImpactUtils.generateGeneId(individual, gene)
        var actionIndex = individual.seeActions().find { it.seeGenes().contains(gene) }.run {
            individual.seeActions().indexOf(this)
        }
        if (actionIndex > 0)
            return impactInfo.actionGeneImpacts[actionIndex][id]

        actionIndex = individual.seeInitializingActions().find { it.seeGenes().contains(gene) }.run {
            individual.seeInitializingActions().indexOf(this)
        }
        if (actionIndex > 0)
            return impactInfo.initializationGeneImpacts[actionIndex][id]

        return null
    }

    private fun initImpacts(){
        getTracking()?.apply {
            assert(size == 0)
        }

        initGeneImpact(ind = individual, includeGenes = individual.seeGenes(Individual.GeneFilter.ONLY_SQL),forInit = true)
        initGeneImpact(ind = individual, forInit = false)

        impactInfo!!.impactsOfStructure.updateStructure(this)

        fitness.getViewOfData().forEach { (t, u) ->
            getReachedTarget()[t] = u.distance
        }
    }

    fun updateGeneDueToAddedInitializationGenes(individual: T, genes : List<Gene>){
        val groupGeneByActionIndex = genes.groupBy {g->
            individual.seeInitializingActions().find { a->a.seeGenes().contains(g) }.run { individual.seeInitializingActions().indexOf(this) }
        }
        if (groupGeneByActionIndex.any { it.key == -1 })
            throw IllegalArgumentException("cannot find at least one of addedInitializationGenes")

        groupGeneByActionIndex.forEach { (actionIndex, u) ->
            impactInfo!!.addOrUpdateGeneImpacts(
                    actionIndex = actionIndex,
                    mutableMap = u.map { g->
                        val id = ImpactUtils.generateGeneId(individual, g)
                        id to ImpactUtils.createGeneImpact(g, id)
                    }.toMap().toMutableMap(),
                    forInit = true,
                    newLine = true
            )
        }
    }

    /**
     * update db gene impacts based on [ind]
     */
    private fun updateDbGenesDueToFixing(ind: T){
        val diff = ind.seeInitializingActions().size - impactInfo!!.initializationGeneImpacts.size
        //truncation
        if (diff < 0){
            val impacts = ind.seeInitializingActions().mapIndexed { index, action ->
                val existing = findGenesImpactsPerAction(action.getName(), index, true)
                if (existing == null) mutableMapOf<String, GeneImpact>()
                else existing
            }.toMutableList()

            impactInfo!!.initializationGeneImpacts.clear()
            impactInfo!!.initializationGeneImpacts.addAll(impacts)
        }else if (diff > 0){
            throw IllegalArgumentException("hand newly added db action")
        }
    }

    private fun findGenesImpactsPerAction(actionName : String, actionIndex : Int, forInit: Boolean) : MutableMap<String, GeneImpact>?{
        val line = if (forInit && impactInfo!!.initializationGeneImpacts.size > actionIndex)
            impactInfo!!.initializationGeneImpacts[actionIndex]
        else if ( !forInit && impactInfo!!.actionGeneImpacts.size > actionIndex)
            impactInfo!!.actionGeneImpacts[actionIndex]
        else
            return null
        val actionNames = line.keys.map { ImpactUtils.extractActionName(it) }.toSet()
        if (actionNames.size !=1)
            throw IllegalArgumentException("mismatched gene impact")
        if (actionNames.first() == actionName)
            return line
        return null
    }

    private fun addGeneForActionIfMissing(ind: T, includeGenes: List<Gene> = listOf(), excludeGenes : List<Gene> = listOf()){

        if (ind.seeActions().isNotEmpty()){
            ind.seeActions().forEachIndexed { index, a->
                a.seeGenes().filter { it.isMutable() && !excludeGenes.contains(it) && (includeGenes.isEmpty() || includeGenes.contains(it)) }.forEach { g->
                    val id = ImpactUtils.generateGeneId(a, g)
                    impactInfo!!.addOrUpdateGeneImpacts(
                            index,
                            id,
                            ImpactUtils.createGeneImpact(g, id),
                            forInit = false,
                            newLine = false
                    )
                }
            }
        }else{
            ind.seeGenes(Individual.GeneFilter.NO_SQL).filter { it.isMutable() && !excludeGenes.contains(it) && (includeGenes.isEmpty() || includeGenes.contains(it)) }.forEach { g->
                val id = ImpactUtils.generateGeneId(ind, g)
                impactInfo!!.addOrUpdateGeneImpacts(
                        0,
                        id,
                        ImpactUtils.createGeneImpact(g, id),
                        forInit = false,
                        newLine = false
                )
            }
        }
    }

    private fun initGeneImpact(ind: T, includeGenes: List<Gene> = listOf(), excludeGenes : List<Gene> = listOf(), forInit : Boolean){
        val actions = if (forInit)
            ind.seeInitializingActions()
        else
            ind.seeActions()

        if (actions.isNotEmpty()){
            actions.forEachIndexed { index, a->
                val maps = a.seeGenes().filter { it.isMutable() && !excludeGenes.contains(it) && (includeGenes.isEmpty() || includeGenes.contains(it)) }.map { g->
                    val id = ImpactUtils.generateGeneId(a, g)
                    id to ImpactUtils.createGeneImpact(g, id)
                }.toMap().toMutableMap()
                impactInfo!!.addOrUpdateGeneImpacts(
                        index,
                        maps,
                        forInit = forInit,
                        newLine = true
                )
            }
        }else{
            val maps = ind.seeGenes(
                    if (forInit) Individual.GeneFilter.ONLY_SQL
                    else Individual.GeneFilter.NO_SQL
            ).filter { it.isMutable() && !excludeGenes.contains(it) && (includeGenes.isEmpty() || includeGenes.contains(it)) }.map { g->
                val id = ImpactUtils.generateGeneId(ind, g)
                id to ImpactUtils.createGeneImpact(g, id)
            }.toMap().toMutableMap()
            impactInfo!!.addOrUpdateGeneImpacts(
                    0,
                    maps,
                    forInit = forInit,
                    newLine = true
            )
        }
    }

    fun anyImpactInfo() : Boolean{
        impactInfo?:return false
        return impactInfo.actionGeneImpacts.isNotEmpty() || impactInfo.initializationGeneImpacts.isNotEmpty()
    }

    fun getGeneImpactById(geneId : String) : List<GeneImpact>{
        impactInfo?:return listOf()
        return impactInfo.getAllGeneImpact(geneId)
    }

    fun flattenAllGeneImpact() : List<GeneImpact>{
        impactInfo?:return listOf()
        return impactInfo.getAllGeneImpact()
    }

}