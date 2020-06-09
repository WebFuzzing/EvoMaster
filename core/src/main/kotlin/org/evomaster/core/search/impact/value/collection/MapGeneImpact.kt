package org.evomaster.core.search.impact.value.collection

import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.MapGene
import org.evomaster.core.search.impact.*
import org.evomaster.core.search.impact.value.numeric.IntegerGeneImpact

/**
 * created by manzh on 2019-09-09
 */
class MapGeneImpact(sharedImpactInfo: SharedImpactInfo, specificImpactInfo: SpecificImpactInfo,
                    val sizeImpact : IntegerGeneImpact
) : CollectionImpact, GeneImpact(sharedImpactInfo, specificImpactInfo){

    constructor(
            id : String,
            degree: Double = 0.0,
            timesToManipulate : Int = 0,
            timesOfNoImpacts : Int = 0,
            timesOfNoImpactWithTargets : MutableMap<Int, Int> = mutableMapOf(),
            timesOfImpact : MutableMap<Int, Int> = mutableMapOf(),
            noImpactFromImpact : MutableMap<Int, Int> = mutableMapOf(),
            noImprovement : MutableMap<Int, Int> = mutableMapOf(),
            sizeImpact : IntegerGeneImpact = IntegerGeneImpact("size")

    ) : this(SharedImpactInfo(id, degree, timesToManipulate, timesOfNoImpacts, timesOfNoImpactWithTargets, timesOfImpact), SpecificImpactInfo(noImpactFromImpact, noImprovement),sizeImpact)

    override fun getSizeImpact(): Impact = sizeImpact

    override fun copy(): MapGeneImpact {
        return MapGeneImpact(
                shared.copy(),
                specific.copy(),
                sizeImpact = sizeImpact.copy())
    }

    override fun clone(): MapGeneImpact {
        return MapGeneImpact(
                shared.clone(),
                specific.clone(),
                sizeImpact.clone()
        )
    }

    override fun countImpactWithMutatedGeneWithContext(gc: MutatedGeneWithContext, noImpactTargets: Set<Int>, impactTargets: Set<Int>, improvedTargets: Set<Int>, onlyManipulation: Boolean) {
        countImpactAndPerformance(noImpactTargets = noImpactTargets, impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation)

        if (gc.previous == null && impactTargets.isNotEmpty()) return
        if(gc.current !is MapGene<*>)
            throw IllegalStateException("gc.current (${gc.current::class.java.simpleName}) should be MapGene")
        if (gc.previous != null && gc.previous !is MapGene<*>)
            throw IllegalStateException("gc.previous (${gc.previous::class.java.simpleName}) should be MapGene")

        if (gc.previous != null && (gc.previous as MapGene<*>).elements.size != gc.current.elements.size)
            sizeImpact.countImpactAndPerformance(noImpactTargets = noImpactTargets, impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation)
    }

    override fun validate(gene: Gene): Boolean = gene is MapGene<*>

    override fun flatViewInnerImpact(): Map<String, Impact> {
        return mutableMapOf("${getId()}-sizeImpact" to sizeImpact)
    }
}