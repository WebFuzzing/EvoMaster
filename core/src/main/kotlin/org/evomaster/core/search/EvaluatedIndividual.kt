package org.evomaster.core.search

import org.evomaster.core.search.gene.*
import org.evomaster.core.search.impact.*
import org.evomaster.core.search.service.mutator.MutatedGeneSpecification
import org.evomaster.core.search.tracer.TraceableElement
import org.evomaster.core.search.tracer.TraceableElementCopyFilter
import org.evomaster.core.search.tracer.TrackOperator
import org.evomaster.core.Lazy

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
                    impactInfo = if (enableImpact) ImpactsOfIndividual(individual, fitness) else null
            ){
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

        val noImpactTargets = next.fitness.getViewOfData().keys.filterNot { impactTargets.contains(it) }.toSet()

        val didStructureMutation = mutatedGenes.didStructureMutation()
        if (didStructureMutation){ // structure mutated
            updateImpactsAfterStructureMutation(next, previous.individual, mutatedGenes, noImpactTargets, impactTargets, improvedTargets)
        }

        /*
            impact info should be always accord with [this].
         */
        if ((!didStructureMutation) || next == this){
            impactInfo!!.syncBasedOnIndividual(individual, mutatedGenes)
        }

        if (didStructureMutation) return

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
        Lazy.assert { impactInfo != null }
        val sizeChanged = (mutatedGenes.mutatedIndividual!!.seeActions().size != previous.seeActions().size)

        //we update genes impact regarding structure only if structure mutated individual is 'next'
        if(this == next){
            if (mutatedGenes.removedGene.isNotEmpty()){ //delete an action
                impactInfo!!.deleteGeneImpacts(fromInitialization = false, actionIndex = mutatedGenes.mutatedPosition.toSet())
            }else if (mutatedGenes.addedGenes.isNotEmpty()){ //add new action
                val groupGeneByActionIndex = mutatedGenes.addedGenes.groupBy {g->
                    mutatedGenes.mutatedIndividual!!.seeActions().find { a->a.seeGenes().contains(g) }.run { mutatedGenes.mutatedIndividual!!.seeActions().indexOf(this) }
                }

                groupGeneByActionIndex.toSortedMap().forEach { (actionIndex, mgenes) ->
                    if (!mutatedGenes.mutatedPosition.contains(actionIndex))
                        throw IllegalArgumentException("mismatched impact info")
                    impactInfo!!.addOrUpdateGeneImpacts(
                            toInitialization = false,
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
                    impactInfo!!.addOrUpdateGeneImpacts(
                            toInitialization = false,
                            actionName = individual.seeActions()[actionIndex].getName(),
                            actionIndex = actionIndex,
                            newAction = true,
                            impacts = mutableMapOf()
                    )
                }else{
                    impactInfo!!.deleteGeneImpacts(fromInitialization = false, actionIndex = setOf(actionIndex))
                }
            }
        }
        //TODO handle other kinds of mutation if it has e.g., replace, exchange
        impactInfo!!.impactsOfStructure?.countImpact(next, sizeChanged, noImpactTargets= noImpactTargets, impactTargets = impactTargets, improvedTargets = improvedTargets)

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
                        onlyManipulation = onlyManipulation
                )
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

    fun getImpactsRelatedTo(mutatedGenes: MutatedGeneSpecification) : List<Impact>{
        impactInfo?:return emptyList()

        if (mutatedGenes.didStructureMutation())
            return emptyList()
        val list = mutableListOf<Impact>()
        mutatedGenes.mutatedGenes.forEachIndexed { index, gene ->
            val actionIndex = mutatedGenes.mutatedPosition[index]
            val action = mutatedGenes.mutatedIndividual!!.seeActions()[actionIndex]
            val id = ImpactUtils.generateGeneId(action, gene)
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
            val id = ImpactUtils.generateGeneId(action, gene)
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
                    actionIndex = individual.seeActions().indexOf(action),
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

    fun updateGeneDueToAddedInitializationGenes(genes : List<Gene>){
        val groupGeneByActionIndex = genes.groupBy {g->
            individual.seeInitializingActions().find { a->a.seeGenes().contains(g) }.run { individual.seeInitializingActions().indexOf(this) }
        }
        if (groupGeneByActionIndex.any { it.key == -1 })
            throw IllegalArgumentException("cannot find at least one of addedInitializationGenes")

        groupGeneByActionIndex.toSortedMap().forEach { (actionIndex, u) ->
            impactInfo!!.addOrUpdateGeneImpacts(
                    toInitialization = true,
                    actionIndex = actionIndex,
                    actionName = individual.seeInitializingActions()[actionIndex].getName(),
                    newAction = true,
                    impacts =  u.map { g->
                        val id = ImpactUtils.generateGeneId(individual, g)
                        id to ImpactUtils.createGeneImpact(g, id)
                    }.toMap().toMutableMap()
            )
        }
    }


    fun anyImpactInfo() : Boolean{
        impactInfo?:return false
        return impactInfo.anyImpactInfo()
    }

    fun flattenAllGeneImpact() : List<GeneImpact>{
        impactInfo?:return listOf()
        return impactInfo.flattenAllGeneImpact()
    }

    fun updateGeneDueToAddedInitializationGenes(evaluatedIndividual: EvaluatedIndividual<*>){
        Lazy.assert {
            impactInfo != null
            evaluatedIndividual.impactInfo != null
        }
        impactInfo!!.updateInitializationGeneImpacts(evaluatedIndividual.impactInfo!!)
    }

    fun getSizeOfActionImpact(fromInitialization : Boolean) : Int{
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
}