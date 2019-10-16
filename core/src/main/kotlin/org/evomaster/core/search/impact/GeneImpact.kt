package org.evomaster.core.search.impact

import org.evomaster.core.search.gene.Gene

/**
 * created by manzh on 2019-09-03
 */

open class GeneImpact (
        id : String,
        degree: Double = 0.0,
        timesToManipulate : Int = 0,
        timesOfNoImpacts : Int = 0,
        timesOfImpact : MutableMap<Int, Int> = mutableMapOf(),
        noImpactFromImpact : MutableMap<Int, Int> = mutableMapOf(),
        noImprovement : MutableMap<Int, Int> = mutableMapOf()
):Impact(
        id = id,
        degree = degree,
        timesToManipulate = timesToManipulate,
        timesOfNoImpacts = timesOfNoImpacts,
        timesOfImpact= timesOfImpact,
        noImpactFromImpact = noImpactFromImpact,
        noImprovement = noImprovement
){
    override fun copy(): GeneImpact {
        return GeneImpact(
                id, degree, timesToManipulate, timesOfNoImpacts, timesOfImpact.toMutableMap(), noImpactFromImpact.toMutableMap(), noImprovement.toMutableMap()
        )
    }

    open fun validate(gene : Gene) : Boolean = true

    open fun countImpactWithMutatedGeneWithContext(gc: MutatedGeneWithContext, impactTargets: Set<Int>, improvedTargets: Set<Int>, onlyManipulation: Boolean){
        countImpactAndPerformance(impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation)
    }

    open fun flatViewInnerImpact(): Map<String, Impact> = mutableMapOf()

}