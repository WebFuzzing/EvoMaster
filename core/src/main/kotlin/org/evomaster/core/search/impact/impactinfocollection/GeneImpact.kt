package org.evomaster.core.search.impact.impactinfocollection

import org.evomaster.core.search.gene.Gene

/**
 * created by manzh on 2019-09-03
 * @property genes refer to genes in their current individual, i.e., you can find what latest updated genes are. this info should not be shared
 */
open class GeneImpact (sharedImpactInfo: SharedImpactInfo, specificImpactInfo: SpecificImpactInfo) : Impact(sharedImpactInfo, specificImpactInfo){

    constructor(
            id : String
    ) : this(SharedImpactInfo(id), SpecificImpactInfo())

    open fun validate(gene : Gene) : Boolean = true

    fun check(previous: Gene?, current: Gene){
        if (previous != null && !validate(previous))
            throw IllegalArgumentException("mismatched gene impact for previous ${previous::class.java}")
        if (!validate(current))
            throw IllegalArgumentException("mismatched gene impact for previous ${current::class.java}")
    }

    fun check(gc: MutatedGeneWithContext){
        check(gc.previous, gc.current)
    }

    open fun countImpactWithMutatedGeneWithContext(gc: MutatedGeneWithContext, noImpactTargets: Set<Int>, impactTargets: Set<Int>, improvedTargets: Set<Int>, onlyManipulation: Boolean){
        countImpactAndPerformance(noImpactTargets = noImpactTargets, impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation, num = gc.numOfMutatedGene)
    }

    /**
     * during search, Gene might be changed due to
     *  e.g., taint analysis, additional info from SUT
     *  thus, we need to sync impact based on current gene
     */
    open fun syncImpact(previous: Gene?, current: Gene){
        check(previous, current)
    }

    open fun innerImpacts() = listOf<Impact>()

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