package org.evomaster.core.search.impact.value

import org.evomaster.core.search.gene.DisruptiveGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.OptionalGene
import org.evomaster.core.search.impact.GeneImpact
import org.evomaster.core.search.impact.Impact
import org.evomaster.core.search.impact.ImpactUtils
import org.evomaster.core.search.impact.MutatedGeneWithContext
import org.evomaster.core.search.impact.value.numeric.BinaryGeneImpact

/**
 * created by manzh on 2019-09-09
 */
class DisruptiveGeneImpact (
        id: String,
        degree: Double = 0.0,
        timesToManipulate: Int = 0,
        timesOfImpact: Int = 0,
        timesOfNoImpacts: Int = 0,
        counter: Int = 0,
        niCounter : Int = 0,
        positionSensitive: Boolean = false,
        val geneImpact: GeneImpact
) : GeneImpact(id, degree, timesToManipulate, timesOfImpact, timesOfNoImpacts, counter, niCounter, positionSensitive) {

    constructor(id : String, gene: DisruptiveGene<*>) : this(id, geneImpact = ImpactUtils.createGeneImpact(gene.gene, id))

    override fun copy(): DisruptiveGeneImpact {
        return DisruptiveGeneImpact(id, degree, timesToManipulate, timesOfImpact, timesOfNoImpacts, counter, niCounter, positionSensitive, geneImpact.copy() as GeneImpact)
    }

    override fun validate(gene: Gene): Boolean = gene is DisruptiveGene<*>

    override fun countImpactWithMutatedGeneWithContext(gc: MutatedGeneWithContext, hasImpact: Boolean, noImprovement: Boolean) {
        countImpactAndPerformance(hasImpact, noImprovement)

        if (gc.previous == null && hasImpact) return
        if (gc.current  !is DisruptiveGene<*>){
            throw IllegalStateException("gc.current (${gc.current::class.java.simpleName}) should be SqlNullable")
        }
        if (gc.previous != null && gc.previous !is DisruptiveGene<*>){
            throw IllegalStateException("gc.previous (${gc.previous::class.java.simpleName}) should be SqlNullable")
        }

        val mutatedGeneWithContext = MutatedGeneWithContext(
                previous = if (gc.previous==null) null else (gc.previous as DisruptiveGene<*>).gene,
                current = gc.current.gene
        )
        geneImpact.countImpactWithMutatedGeneWithContext(mutatedGeneWithContext, hasImpact, noImprovement)
    }

    override fun flatViewInnerImpact(): Map<String, Impact> {
        return mutableMapOf(
                "$id-geneImpact" to geneImpact
        ).plus(geneImpact.flatViewInnerImpact())
    }
}