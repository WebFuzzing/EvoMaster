package org.evomaster.core.search.impact.value.collection

import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.MapGene
import org.evomaster.core.search.impact.GeneImpact
import org.evomaster.core.search.impact.Impact
import org.evomaster.core.search.impact.MutatedGeneWithContext
import org.evomaster.core.search.impact.value.numeric.IntegerGeneImpact

/**
 * created by manzh on 2019-09-09
 */
class MapGeneImpact (
        id : String,
        degree: Double = 0.0,
        timesToManipulate : Int = 0,
        timesOfNoImpacts : Int = 0,
        timesOfImpact : MutableMap<Int, Int> = mutableMapOf(),
        noImpactFromImpact : MutableMap<Int, Int> = mutableMapOf(),
        noImprovement : MutableMap<Int, Int> = mutableMapOf(),
        val sizeImpact : IntegerGeneImpact = IntegerGeneImpact("size")
)  : GeneImpact(
        id = id,
        degree = degree,
        timesToManipulate = timesToManipulate,
        timesOfNoImpacts = timesOfNoImpacts,
        timesOfImpact= timesOfImpact,
        noImpactFromImpact = noImpactFromImpact,
        noImprovement = noImprovement
) {

    override fun copy(): MapGeneImpact {
        return MapGeneImpact(
                id = id,
                degree = degree,
                timesToManipulate = timesToManipulate,
                timesOfNoImpacts = timesOfNoImpacts,
                timesOfImpact= timesOfImpact.toMutableMap(),
                noImpactFromImpact = noImpactFromImpact.toMutableMap(),
                noImprovement = noImprovement.toMutableMap(),
                sizeImpact = sizeImpact.copy())
    }

    override fun countImpactWithMutatedGeneWithContext(gc: MutatedGeneWithContext, impactTargets: Set<Int>, improvedTargets: Set<Int>, onlyManipulation: Boolean) {
        countImpactAndPerformance(impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation)

        if (gc.previous == null && impactTargets.isNotEmpty()) return
        if(gc.current !is MapGene<*>)
            throw IllegalStateException("gc.current (${gc.current::class.java.simpleName}) should be MapGene")
        if (gc.previous != null && gc.previous !is MapGene<*>)
            throw IllegalStateException("gc.previous (${gc.previous::class.java.simpleName}) should be MapGene")

        if (gc.previous != null && (gc.previous as MapGene<*>).elements.size != gc.current.elements.size)
            sizeImpact.countImpactAndPerformance(impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation)
    }

    override fun validate(gene: Gene): Boolean = gene is MapGene<*>

    override fun flatViewInnerImpact(): Map<String, Impact> {
        return mutableMapOf("$id-sizeImpact" to sizeImpact)
    }
}