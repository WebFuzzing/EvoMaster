package org.evomaster.core.search

import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.impact.ImpactOfGene
import org.evomaster.core.search.impact.ImpactOfStructure
import org.evomaster.core.search.service.Sampler
import org.evomaster.core.search.tracer.TraceableElement
import org.evomaster.core.search.tracer.TrackOperator
import kotlin.math.absoluteValue

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
                             withImpacts : Boolean = false)
    : TraceableElement(trackOperator,  tracking, undoTracking) where T : Individual {

    init{
        if(individual.seeActions().size < results.size){
            throw IllegalArgumentException("Less actions than results")
        }
        if(tracking!=null && tracking.isNotEmpty() && tracking.first().trackOperator !is Sampler<*>){
            throw IllegalArgumentException("first tracking operator should be a sampler")
        }
        if(withImpacts)
            initImpacts()
    }

    /**
     * key -> action name : gene name
     * value -> impact degree
     */
    val impactsOfGenes : MutableMap<String, ImpactOfGene> = mutableMapOf()

    /**
     * key -> action names that join with ";"
     * value -> impact degree
     */
    val impactsOfStructure : MutableMap<String, ImpactOfStructure> = mutableMapOf()

    private val reachedTargets : MutableMap<Int, Double> = mutableMapOf()
    /**
     * [hasImprovement] represents if [this] helps to improve Archive, e.g., reach new target.
     */
    var hasImprovement = false

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

    override fun copy(withTrack: Boolean): EvaluatedIndividual<T> {

        when(withTrack){
            false-> return copy()
            else ->{
                /**
                 * if the [getTracking] is null, which means the tracking option is attached on individual not evaluated individual
                 */
                getTracking()?:return EvaluatedIndividual(
                        fitness.copy(),
                        individual.copy(withTrack) as T,
                        results.map(ActionResult::copy),
                        trackOperator
                )

                return forceCopyWithTrack()
            }
        }
    }

    fun forceCopyWithTrack(): EvaluatedIndividual<T> {
        val copy = EvaluatedIndividual(
                fitness.copy(),
                individual.copy() as T,
                results.map(ActionResult::copy),
                trackOperator?:individual.trackOperator,
                getTracking()?.map { it.copy() }?.toMutableList()?: mutableListOf(),
                getUndoTracking()?.map { it.copy()}?.toMutableList()?: mutableListOf()
        )

        copyWithImpacts(copy)
        return copy
    }

    fun getHistoryOfGene(gene: Gene) : List<Gene>{
        val geneId = GeneIdUtil.generateGeneId(individual, gene)
        getTracking()?: throw IllegalArgumentException("tracking is not enabled")
        return getTracking()!!.flatMap { it.individual.seeGenes().find { g->GeneIdUtil.generateGeneId(it.individual, g) == geneId}.run {
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

        val geneId = GeneIdUtil.generateGeneId(individual, gene)

        //the individual was mutated in terms of structure, the gene might be found in history
        val latest = latestInd.seeGenes().find { GeneIdUtil.generateGeneId(latestInd, it) == geneId }?:return null
        return if (latest::class.java.simpleName == gene::class.java.simpleName) latest else null
    }

    private fun copyWithImpacts(copy : EvaluatedIndividual<T>) {
        copy.impactsOfGenes.putAll(impactsOfGenes.map { it.key to it.value.copy() }.toMap())
        copy.impactsOfStructure.putAll(impactsOfStructure.map { it.key to it.value.copy() as ImpactOfStructure }.toMap())
        copy.reachedTargets.putAll(reachedTargets.map { it.key to it.value }.toMap())
    }


    override fun next(trackOperator: TrackOperator, next: TraceableElement): EvaluatedIndividual<T>? {
        if (next !is EvaluatedIndividual<*>) throw  IllegalArgumentException("the type of next is mismatched")
        val copy =  EvaluatedIndividual(
                next.fitness.copy(),
                next.individual.copy(false) as T,
                next.results.map(ActionResult::copy),
                trackOperator,
                getTracking()?.plus(this)?.map { it.copy()}?.toMutableList()?: mutableListOf(this.copy()),
                getUndoTracking()?.map { it.copy()}?.toMutableList()?: mutableListOf()
        )

        copyWithImpacts(copy)

        return copy
    }

    override fun getUndoTracking(): MutableList<EvaluatedIndividual<T>>? {
        if(super.getUndoTracking() == null) return null
        return super.getUndoTracking() as MutableList<EvaluatedIndividual<T>>
    }

    private fun initImpacts(){
        getTracking()?.apply {
            assert(size == 0)
        }
        individual.seeActions().forEach { a->
            a.seeGenes().forEach { g->
                val id = GeneIdUtil.generateGeneId(a, g)
                impactsOfGenes.putIfAbsent(id, ImpactOfGene(id, 0.0))
            }
        }
        /*
            empty action?
         */
        if(individual.seeActions().isEmpty()){
            individual.seeGenes().forEach {
                val id = GeneIdUtil.generateGeneId(it)
                impactsOfGenes.putIfAbsent(id, ImpactOfGene(id, 0.0))
            }
        }

        getTracking()?.apply {
            assert(size == 0)
        }

        if(individual.seeActions().isNotEmpty()){
            val id = GeneIdUtil.generateIndividualId(individual)
            impactsOfStructure.putIfAbsent(id, ImpactOfStructure(id, 0.0))
        }

        fitness.getViewOfData().forEach { t, u ->
            reachedTargets.put(t, u.distance)
        }
    }

    /**
     * compare current with latest
     * [inTrack] indicates how to find the latest two elements to compare.
     * For instance, if the latest modification does not improve the fitness, it will be saved in [undoTracking].
     * in this case, the latest is the last of [undoTracking], not [this]
     */
    fun updateImpactOfGenes(inTrack : Boolean){
        assert(getTracking() != null)
        assert(getUndoTracking() != null)

        if(inTrack) assert(getTracking()!!.isNotEmpty())
        else assert(getUndoTracking()!!.isNotEmpty())

        val previous = if(inTrack) getTracking()!!.last() else this
        val next = if(inTrack) this else getUndoTracking()!!.last()

        val isAnyOverallImprovement = updateReachedTargets(fitness)
        val comparedFitness = next.fitness.computeFitnessScore() - previous.fitness.computeFitnessScore()

        compareWithLatest(next, previous, (isAnyOverallImprovement || comparedFitness != 0.0), inTrack)
    }

    private fun compareWithLatest(next : EvaluatedIndividual<T>, previous : EvaluatedIndividual<T>, isAnyChange : Boolean, isNextThis : Boolean){
        val delta = (next.fitness.computeFitnessScore() - previous.fitness.computeFitnessScore()).absoluteValue

        if(impactsOfStructure.isNotEmpty()){
            val nextSeq = GeneIdUtil.generateIndividualId(next.individual)
            val previousSeq = GeneIdUtil.generateIndividualId(previous.individual)
            /*
                a sequence of an individual is used to present its structure,
                    the degree of impact of the structure is evaluated as the max fitness value.
                In this case, we only find best and worst structure.
             */
            val structureId = if(isNextThis)nextSeq else previousSeq
            if(nextSeq != previousSeq){
                val impact = impactsOfStructure.getOrPut(structureId){ImpactOfStructure(structureId, 0.0)}
                val degree = (if(isNextThis)next else previous).fitness.computeFitnessScore()
                if( degree > impact.degree)
                    impact.degree = degree
            }
            impactsOfStructure[structureId]!!.countImpact(isAnyChange)
        }

        val geneIds = generateMap(next.individual)
        val latestIds = generateMap(previous.individual)

        //following is to detect genes to mutate, and update the impact of the detected genes.
        /*
            same gene, but the value is different.
            In this case, we increase the degree impact based on
                absoluteValue of difference between next and previous regarding fitness, i.e., delta
         */
        geneIds.keys.intersect(latestIds.keys).forEach { keyId->
            val curGenes = GeneIdUtil.extractGeneById(next.individual.seeActions(), keyId)
            val latestGenes = GeneIdUtil.extractGeneById(previous.individual.seeActions(), keyId)

            curGenes.forEach { cur ->
                latestGenes.find { GeneIdUtil.isAnyChange(cur, it) }?.let {
                    impactsOfGenes.getOrPut(keyId){
                        ImpactOfGene(keyId, 0.0)
                    }.apply{
                        if(isAnyChange) increaseDegree(delta)
                        countImpact(isAnyChange)
                    }
                }
            }

        }

        /*
            new gene, we increase its impact by delta
         */
        geneIds.filter { !latestIds.containsKey(it.key) }.forEach { t, _ ->
            impactsOfGenes.getOrPut(t){
                ImpactOfGene(t, 0.0)
            }.apply{
                if(isAnyChange) increaseDegree(delta)
                countImpact(isAnyChange)
            }
        }

        /*
           removed gene, we increase its impact by delta
        */
        latestIds.filter { !geneIds.containsKey(it.key) }.forEach { t, _ ->
            impactsOfGenes.getOrPut(t){
                ImpactOfGene(t, 0.0)
            }.apply{
                if(isAnyChange) increaseDegree(delta)
                countImpact(isAnyChange)
            }
        }
    }


    private fun generateMap(individual: T) : MutableMap<String, MutableList<Gene>>{
        val map = mutableMapOf<String, MutableList<Gene>>()
        if(individual.seeActions().isNotEmpty()){
            individual.seeActions().forEachIndexed {_, a ->
                a.seeGenes().forEach {g->
                    val genes = map.getOrPut(GeneIdUtil.generateGeneId(a, g)){ mutableListOf()}
                    genes.add(g)
                }
            }
        }else{
            individual.seeGenes().forEach {g->
                val genes = map.getOrPut(GeneIdUtil.generateGeneId(g)){ mutableListOf()}
                genes.add(g)
            }
        }

        return map
    }

    private fun updateReachedTargets(fitness: FitnessValue) : Boolean{
        var isAnyOverallImprovement = false
        fitness.getViewOfData().forEach { t, u ->
            var previous = reachedTargets[t]
            if(previous == null){
                isAnyOverallImprovement = true
                previous = 0.0
                reachedTargets.put(t, previous)
            }
            isAnyOverallImprovement = isAnyOverallImprovement || u.distance > previous
            if(u.distance > previous)
                reachedTargets[t] = u.distance
        }
        return isAnyOverallImprovement
    }

    override fun getTracking(): List<EvaluatedIndividual<T>>? {
        val tacking = super.getTracking()?:return null
        if(tacking.all { it is EvaluatedIndividual<*> })
            return tacking as List<EvaluatedIndividual<T>>
        else
            throw IllegalArgumentException("tracking has elements with mismatched type")
    }
}