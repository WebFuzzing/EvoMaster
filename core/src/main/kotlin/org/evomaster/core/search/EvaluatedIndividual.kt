package org.evomaster.core.search

import org.evomaster.core.search.service.tracer.TraceableElement

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

    companion object {
        const val SEPARATOR_GENE_ID = "::"
    }

    val impactsOfGenes : MutableMap<String, Double> = mutableMapOf()
    val impactsOfStructure : MutableMap<String, Double> = mutableMapOf()

    var makeAnyBetter = false

    /**
     * rules to score impact value of genes, baseline is always fitness of first evaluated individual
     * if only standard mutator is accepted, then ids of impactsOfGenes are not changed during search
     * but if structure mutator is also accepted, then ids of impactOfGenes are changed regarding added or removal of genes
     */
    fun updateImpactOfGenes(next: EvaluatedIndividual<T>){
        if(impactsOfGenes.isEmpty()){
            val initValue = fitness.computeFitnessScore()
            next.individual.seeActions().distinctBy { it.getName() }.forEach { na ->
                na.seeGenes().forEach { nag ->
                    val id = nag.getVariableName()+ SEPARATOR_GENE_ID + na.getName()
                    impactsOfGenes.putIfAbsent(id, initValue)
                }
            }
        }
        val targets = fitness.getViewOfData().keys.plus(next.fitness.getViewOfData().keys)
        if(next.fitness.subsumes(fitness, targets) || fitness.subsumes(next.fitness, targets)){
            val value = next.fitness.computeFitnessScore()
            next.individual.seeActions().forEach { na ->
                na.seeGenes().forEach { nag ->
                    val id = nag.getVariableName()+ SEPARATOR_GENE_ID + na.getName()
                    val sameGenes = individual.seeActions().filter { a->a.getName()==na.getName() }.flatMap { a->a.seeGenes() }
                            .filter { ag ->ag.getVariableName() == nag.getVariableName() }
                    if(sameGenes.isEmpty()){
                        increaseGeneImpact(id, value)
                    }else{
                        sameGenes.find { it.containsSameValueAs(nag) }?.apply {
                            increaseGeneImpact(id, value)
                        }
                    }
                }
            }
        }
    }

    private fun increaseGeneImpact(key : String, delta : Double){
        val previous = impactsOfGenes.getOrPut(key){0.0}
        impactsOfGenes.replace(key, previous + delta)
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

                val copyTraces = mutableListOf<EvaluatedIndividual<T>>()
                getTrack()?.forEach {
                    copyTraces.add((it as EvaluatedIndividual<T> ).copy())
                }

                val copyUndoTraces = mutableListOf<EvaluatedIndividual<T>>()
                undoTrack?.forEach {
                    copyUndoTraces.add(it.copy())
                }
                return EvaluatedIndividual(
                        fitness.copy(),
                        individual.copy(withTrack) as T,
                        results.map(ActionResult::copy),
                        getDescription(),
                        copyTraces,
                        copyUndoTraces
                )
            }
        }

    }

    override fun next(description: String, next: TraceableElement): EvaluatedIndividual<T>? {
        if(next !is EvaluatedIndividual<*>) return null
        when(isCapableOfTracking()){
            false-> return null
            else ->{
                val size = getTrack()?.size?: 0
                val copyTraces = mutableListOf<EvaluatedIndividual<T>>()
                (0 until if(maxlength != -1 && size > maxlength - 1) maxlength-1  else size).forEach {
                    copyTraces.add(0, (getTrack()!![size-1-it] as EvaluatedIndividual<T> ).copy())
                }
                copyTraces.add(this.copy())
                return EvaluatedIndividual(
                        next.fitness.copy(),
                        next.individual.copy(true) as T,
                        next.results.map(ActionResult::copy),
                        description,
                        copyTraces
                )
            }
        }
    }


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
                results.map(ActionResult::copy)
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

    class ImpactOfGene(val step : Int, val id : String, var impact : Double)
    class ImpactOfIndividual(val impacts: MutableMap<String, ImpactOfGene> = mutableMapOf(), var impactOfStructure: Double = -1.0)
    class ImpactOfStructure(val impacts: MutableMap<String, ImpactOfIndividual> = mutableMapOf())
}