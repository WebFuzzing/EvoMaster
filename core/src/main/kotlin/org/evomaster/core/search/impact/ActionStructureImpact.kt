package org.evomaster.core.search.impact

import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.impact.value.numeric.IntegerGeneImpact

/**
 * created by manzh on 2019-09-03
 */
class ActionStructureImpact  (sharedImpactInfo: SharedImpactInfo, specificImpactInfo: SpecificImpactInfo,
                              val sizeImpact : IntegerGeneImpact,
                              val structures : MutableMap<String, Double>
) : GeneImpact(sharedImpactInfo, specificImpactInfo){

    constructor(
            id : String,
            degree: Double = 0.0,
            timesToManipulate : Int = 0,
            timesOfNoImpacts : Int = 0,
            timesOfNoImpactWithTargets : MutableMap<Int, Int> = mutableMapOf(),
            timesOfImpact : MutableMap<Int, Int> = mutableMapOf(),
            noImpactFromImpact : MutableMap<Int, Int> = mutableMapOf(),
            noImprovement : MutableMap<Int, Int> = mutableMapOf(),
            sizeImpact : IntegerGeneImpact = IntegerGeneImpact("size"),
            structures : MutableMap<String, Double> = mutableMapOf()
    ) : this(
            SharedImpactInfo(id, degree, timesToManipulate, timesOfNoImpacts, timesOfNoImpactWithTargets, timesOfImpact),
            SpecificImpactInfo(noImpactFromImpact, noImprovement),
            sizeImpact, structures
    )


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


    fun countImpact(evaluatedIndividual : EvaluatedIndividual<*>, sizeChanged : Boolean, noImpactTargets: Set<Int>, impactTargets : Set<Int>, improvedTargets : Set<Int>, onlyManipulation : Boolean = false){
        countImpactAndPerformance(noImpactTargets = noImpactTargets, impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation)
        if (sizeChanged) sizeImpact.countImpactAndPerformance(noImpactTargets = noImpactTargets, impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation)
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
        return ActionStructureImpact(
                shared.copy(),
                specific.copy(),
                sizeImpact = sizeImpact.copy(),
                structures = structures.map { Pair(it.key, it.value) }.toMap().toMutableMap())
    }

    override fun clone(): ActionStructureImpact {
        return ActionStructureImpact(
                shared.clone(),
                specific.clone(),
                sizeImpact = sizeImpact.clone(),
                structures = structures.map { Pair(it.key, it.value) }.toMap().toMutableMap())
    }
}