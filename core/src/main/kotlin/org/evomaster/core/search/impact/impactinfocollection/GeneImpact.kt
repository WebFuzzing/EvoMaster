package org.evomaster.core.search.impact.impactinfocollection

import org.evomaster.core.search.gene.Gene

/**
 * created by manzh on 2019-09-03
 * @property genes refer to genes in their current individual, i.e., you can find what latest updated genes are. this info should not be shared
 */
open class GeneImpact (sharedImpactInfo: SharedImpactInfo, specificImpactInfo: SpecificImpactInfo) : Impact(sharedImpactInfo, specificImpactInfo){

    constructor(
            id : String,
            degree: Double = 0.0,
            timesToManipulate : Int = 0,
            timesOfNoImpacts : Int = 0,
            timesOfNoImpactWithTargets : MutableMap<Int, Int> = mutableMapOf(),
            timesOfImpact : MutableMap<Int, Int> = mutableMapOf(),
            noImpactFromImpact : MutableMap<Int, Int> = mutableMapOf(),
            noImprovement : MutableMap<Int, Int> = mutableMapOf()
    ) : this(SharedImpactInfo(id, degree, timesToManipulate, timesOfNoImpacts, timesOfNoImpactWithTargets, timesOfImpact), SpecificImpactInfo(noImpactFromImpact, noImprovement))

    open fun validate(gene : Gene) : Boolean = true

    open fun countImpactWithMutatedGeneWithContext(gc: MutatedGeneWithContext, noImpactTargets: Set<Int>, impactTargets: Set<Int>, improvedTargets: Set<Int>, onlyManipulation: Boolean){
        countImpactAndPerformance(noImpactTargets = noImpactTargets, impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation)
    }

    open fun flatViewInnerImpact(): Map<String, Impact> = mutableMapOf()

    override fun copy(): GeneImpact {
        return GeneImpact(
                shared.copy(), specific.copy()
        )
    }
    override fun clone() : GeneImpact{
        return GeneImpact(
                shared.clone(), specific.clone()
        )
    }

}