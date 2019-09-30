package org.evomaster.core.search.impact.value.collection

import org.evomaster.core.search.gene.ArrayGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.MapGene
import org.evomaster.core.search.impact.GeneImpact
import org.evomaster.core.search.impact.MutatedGeneWithContext
import org.evomaster.core.search.impact.value.numeric.IntegerGeneImpact

/**
 * created by manzh on 2019-09-09
 */
class ArrayGeneImpact (
        id: String,
        degree: Double = 0.0,
        timesToManipulate: Int = 0,
        timesOfImpact: Int = 0,
        timesOfNoImpacts: Int = 0,
        counter: Int = 0,
        positionSensitive: Boolean = false,
        val sizeImpact : IntegerGeneImpact = IntegerGeneImpact("size")
) : GeneImpact(id, degree, timesToManipulate, timesOfImpact, timesOfNoImpacts, counter, positionSensitive) {

    override fun copy(): ArrayGeneImpact {
        return ArrayGeneImpact(id, degree, timesToManipulate, timesOfImpact, timesOfNoImpacts, counter, positionSensitive, sizeImpact.copy())
    }

    override fun countImpactWithMutatedGeneWithContext(gc: MutatedGeneWithContext, hasImpact: Boolean, noImprovement: Boolean) {
        countImpactAndPerformance(hasImpact, noImprovement)
        if (gc.previous == null && hasImpact) return
        if (gc.current !is ArrayGene<*>)
            throw IllegalStateException("gc.current (${gc.current::class.java.simpleName}) should be ArrayGene")
        if ((gc.previous != null && gc.previous !is ArrayGene<*>))
            throw IllegalStateException("gc.previous (${gc.previous::class.java.simpleName}) should be ArrayGene")

        if (gc.previous == null || (gc.previous as ArrayGene<*>).elements.size != gc.current.elements.size)
            sizeImpact.countImpactAndPerformance(hasImpact, noImprovement)
    }

    override fun validate(gene: Gene): Boolean = gene is ArrayGene<*>
}