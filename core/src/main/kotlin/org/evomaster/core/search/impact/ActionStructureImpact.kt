package org.evomaster.core.search.impact

import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.impact.value.numeric.IntegerGeneImpact

/**
 * created by manzh on 2019-09-03
 */
class ActionStructureImpact (
        id: String,
        degree: Double = 0.0,
        timesToManipulate: Int = 0,
        timesOfImpact: Int = 0,
        timesOfNoImpacts: Int = 0,
        counter: Int = 0,
        val sizeImpact : IntegerGeneImpact = IntegerGeneImpact("size"),
        val structures : MutableMap<String, Double> = mutableMapOf()
): Impact(id, degree, timesToManipulate, timesOfImpact, timesOfNoImpacts, counter){

    companion object{
        const val ACTION_SEPARATOR = ";"
    }

    /*
       a sequence of actions of an individual is used to present its structure,
           the degree of impact of the structure is evaluated as the max fitness value.
       In this case, we only identify best and worst structure.
    */
    fun updateStructure(evaluatedIndividual : EvaluatedIndividual<*>){
        val structureId = evaluatedIndividual.individual.seeActions().joinToString(ACTION_SEPARATOR){it.getName()}
        val impact = structures.getOrPut(structureId){ 0.0 }
        val fitness = evaluatedIndividual.fitness.computeFitnessScore()
        if ( fitness > impact ) structures[structureId] = fitness
    }


    fun countImpact(evaluatedIndividual : EvaluatedIndividual<*>, hasImpacts : Boolean, sizeChanged : Boolean, isWorse : Boolean){
        countImpactAndPerformance(hasImpacts, isWorse)
        if (sizeChanged) sizeImpact.countImpactAndPerformance(hasImpacts, isWorse)
        updateStructure(evaluatedIndividual)
    }

    fun getStructures(top : Int = 1) : List<List<String>>{
        if (top > structures.size)
            throw IllegalArgumentException("$top is more than the size of existing structures")
        val sorted = structures.asSequence().sortedBy { it.value }.toList()
        return sorted.subList(0, top).map { it.key.split(ACTION_SEPARATOR)  }
    }

    fun getStructures(minimalFitness : Double) : List<List<String>>{
        return structures.filter { it.value >= minimalFitness }.keys.map { it.split(ACTION_SEPARATOR) }
    }

    override fun copy() : ActionStructureImpact{
        return ActionStructureImpact(id, degree, timesToManipulate, timesOfImpact, timesOfNoImpacts, counter, sizeImpact.copy(), structures.map { Pair(it.key, it.value) }.toMap().toMutableMap())
    }
}