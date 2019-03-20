package org.evomaster.core.search

import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.service.impact.ImpactOfGene
import org.evomaster.core.search.service.impact.ImpactOfStructure
import org.evomaster.core.search.service.tracer.TraceableElement
import kotlin.math.absoluteValue

/**
 * EvaluatedIndividual allows to track its evolution.
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
                             nextDescription: String? = null,
                             previous : MutableList<EvaluatedIndividual<T>>? = null,
                             val undoTrack : MutableList<EvaluatedIndividual<T>>? = null
)
    : TraceableElement(nextDescription?:individual.getDescription(),  previous)
where T : Individual {

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

    val reachedTargets : MutableMap<Int, Double> = mutableMapOf()

    var makeAnyBetter = false


    fun initImpacts(){
        getTrack()?.apply {
            assert(size == 0)
        }
        individual.seeActions().forEach { a->
            a.seeGenes().forEach { g->
                val id = ImpactOfGene.generateId(a, g)
                impactsOfGenes.putIfAbsent(id, ImpactOfGene(id, 0.0))
            }
        }
        /*
            empty action?
         */
        if(individual.seeActions().isEmpty()){
            individual.seeGenes().forEach {
                val id = ImpactOfGene.generateId(it)
                impactsOfGenes.putIfAbsent(id, ImpactOfGene(id, 0.0))
            }
        }

        getTrack()?.apply {
            assert(size == 0)
        }

        if(individual.seeActions().isNotEmpty()){
            val id = ImpactOfStructure.generateId(individual)
            impactsOfStructure.putIfAbsent(id, ImpactOfStructure(id, 0.0))
        }

        fitness.getViewOfData().forEach { t, u ->
            reachedTargets.put(t, u.distance)
        }
    }

    /**
     * compare current with latest
     */
    fun updateImpactOfGenes(inTrack : Boolean){
        assert(getTrack() != null)
        assert(undoTrack != null)

        if(inTrack) assert(getTrack()!!.isNotEmpty())
        else assert(undoTrack!!.isNotEmpty())

        val previous = if(inTrack) getTrack()!!.last() as EvaluatedIndividual<T> else this
        val next = if(inTrack) this else undoTrack!!.last()

        val isAnyOverallImprovement = updateReachedTargets(fitness)
        val comparedFitness = next.fitness.computeFitnessScore() - previous.fitness.computeFitnessScore()

        compareWithLatest(next, previous, (isAnyOverallImprovement || comparedFitness != 0.0), inTrack)
    }

    private fun compareWithLatest(next : EvaluatedIndividual<T>, previous : EvaluatedIndividual<T>, isAnyChange : Boolean, isNextThis : Boolean){
        val delta = (next.fitness.computeFitnessScore() - previous.fitness.computeFitnessScore()).absoluteValue

        if(impactsOfStructure.isNotEmpty()){

            val nextSeq = ImpactOfStructure.generateId(next.individual)
            val previousSeq = ImpactOfStructure.generateId(previous.individual)

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
            val curGenes = ImpactOfGene.extractGeneById(next.individual.seeActions(), keyId)
            val latestGenes = ImpactOfGene.extractGeneById(previous.individual.seeActions(), keyId)

            curGenes.forEach { cur ->
                latestGenes.find { ImpactOfGene.isAnyChange(cur, it) }?.let {
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
            individual.seeActions().forEachIndexed {i, a ->
                a.seeGenes().forEach {g->
                    val genes = map.getOrPut(ImpactOfGene.generateId(a, g)){ mutableListOf()}
                    genes.add(g)
                }
            }
        }else{
            individual.seeGenes().forEach {g->
                val genes = map.getOrPut(ImpactOfGene.generateId(g)){ mutableListOf()}
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

    override fun copy(withTrack: Boolean): EvaluatedIndividual<T> {

        when(withTrack){
            false-> return copy()
            else ->{
                getTrack()?:return EvaluatedIndividual(
                        fitness.copy(),
                        individual.copy(withTrack) as T,
                        results.map(ActionResult::copy)
                )

               return deepCopy()
            }
        }

    }

    private fun deepCopy() : EvaluatedIndividual<T>{
        val copyTraces = getTrack()?.map { (it as EvaluatedIndividual<T> ).copy() }?.toMutableList()?: mutableListOf()
        val copyUndoTraces = undoTrack?.map {it.copy()}?.toMutableList()?: mutableListOf()

        val copy =  EvaluatedIndividual(
                fitness.copy(),
                individual.copy() as T,
                results.map(ActionResult::copy),
                getDescription(),
                copyTraces,
                copyUndoTraces
        )

        if(impactsOfGenes.isNotEmpty()) copy.impactsOfGenes.putAll(impactsOfGenes.map { it.key to it.value.copy() }.toMap())
        if(impactsOfStructure.isNotEmpty()) copy.impactsOfStructure.putAll(impactsOfStructure.map { it.key to it.value.copy() }.toMap())
        if(reachedTargets.isNotEmpty()) copy.reachedTargets.putAll(reachedTargets.map { it.key to it.value }.toMap())

        return copy
    }

    override fun next(description: String, next: TraceableElement): EvaluatedIndividual<T>? {
        if(next !is EvaluatedIndividual<*>) return null
        when(isCapableOfTracking()){
            false-> return null
            else ->{
                //TODO MAN maxsize of traces
                val copyTraces = getTrack()?.map { (it as EvaluatedIndividual<T> ).copy() }?.toMutableList()?: mutableListOf()
                copyTraces.add(this.copy())
                val copyUndoTraces = undoTrack?.map {it.copy()}?.toMutableList()?: mutableListOf()

                val copy =  EvaluatedIndividual(
                        next.fitness.copy(),
                        next.individual.copy(true) as T,
                        next.results.map(ActionResult::copy),
                        description,
                        copyTraces,
                        copyUndoTraces
                )

                copy.impactsOfGenes.putAll(impactsOfGenes.map { it.key to it.value.copy() as ImpactOfGene }.toMap())
                copy.impactsOfStructure.putAll(impactsOfStructure.map { it.key to it.value.copy() as ImpactOfStructure }.toMap())
                copy.reachedTargets.putAll(reachedTargets.map { it.key to it.value }.toMap())

                return copy
            }
        }
    }

//    fun nextIsUndo(undo : EvaluatedIndividual<T>): EvaluatedIndividual<T> {
//        if(undoTrack == null){
//            val copy = deepCopy()
//            copy.undoTrack!!.add(undo.copy())
//            return copy
//        }else{
//            undoTrack.add(undo.copy(false))
//            return this
//        }
//    }


    override fun isCapableOfTracking(): Boolean = true

    init{
        if(individual.seeActions().size < results.size){
            throw IllegalArgumentException("Less actions than results")
        }
    }

    fun copy(): EvaluatedIndividual<T> {
        return EvaluatedIndividual(
                fitness.copy(),
                individual.copy() as T,
                results.map(ActionResult::copy),
                getDescription()
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

    fun percentageOfGenesToMutate() : Double?{
        if(impactsOfGenes.isEmpty()) return null
        return impactsOfGenes.filter { it.value.timesToManipulate > 0 }.size.toDouble()/impactsOfGenes.size
    }

    fun percentageOfGenesToMutate(geneIds : List<String>) : Double?{
        impactsOfGenes.filter { geneIds.contains(it.key) }.apply {
            return  if(isEmpty())  null
                    else filter { it.value.timesToManipulate > 0 }.size.toDouble()/size
        }
    }

}