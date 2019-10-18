package org.evomaster.core.search.impact.value

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
class OptionalGeneImpact (
        id : String,
        degree: Double = 0.0,
        timesToManipulate : Int = 0,
        timesOfNoImpacts : Int = 0,
        timesOfImpact : MutableMap<Int, Int> = mutableMapOf(),
        noImpactFromImpact : MutableMap<Int, Int> = mutableMapOf(),
        noImprovement : MutableMap<Int, Int> = mutableMapOf(),
        val activeImpact : BinaryGeneImpact = BinaryGeneImpact("isActive"),
        val geneImpact: GeneImpact
)  : GeneImpact(
        id = id,
        degree = degree,
        timesToManipulate = timesToManipulate,
        timesOfNoImpacts = timesOfNoImpacts,
        timesOfImpact= timesOfImpact,
        noImpactFromImpact = noImpactFromImpact,
        noImprovement = noImprovement
) {

    constructor(id : String, optionalGene: OptionalGene) : this(id, geneImpact = ImpactUtils.createGeneImpact(optionalGene.gene, id))

    override fun copy(): OptionalGeneImpact {
        return OptionalGeneImpact(
                id = id,
                degree = degree,
                timesToManipulate = timesToManipulate,
                timesOfNoImpacts = timesOfNoImpacts,
                timesOfImpact= timesOfImpact.toMutableMap(),
                noImpactFromImpact = noImpactFromImpact.toMutableMap(),
                noImprovement = noImprovement.toMutableMap(),
                activeImpact = activeImpact.copy(),
                geneImpact = geneImpact.copy() as GeneImpact)
    }

    override fun countImpactWithMutatedGeneWithContext(gc: MutatedGeneWithContext, impactTargets: Set<Int>, improvedTargets: Set<Int>, onlyManipulation: Boolean) {
        countImpactAndPerformance(impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation)

        if (gc.current !is OptionalGene)
            throw IllegalStateException("gc.current(${gc.current::class.java.simpleName}) should be OptionalGene")

        if (gc.previous != null && gc.previous !is OptionalGene)
            throw IllegalStateException("gc.pervious (${gc.previous::class.java.simpleName}) should be OptionalGene")

        if (gc.previous == null || (gc.previous as OptionalGene).isActive != gc.current.isActive){
            activeImpact.countImpactAndPerformance(impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation)
            if (gc.current.isActive)
                activeImpact.trueValue.countImpactAndPerformance(impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation)
            else
                activeImpact.falseValue.countImpactAndPerformance(impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation)

            if (gc.previous != null){
                return
            }
        }

        if (gc.previous == null && impactTargets.isNotEmpty()) return

        if (gc.current.isActive){
            val mutatedGeneWithContext = MutatedGeneWithContext(
                    previous = if (gc.previous==null) null else (gc.previous as OptionalGene).gene,
                    current = gc.current.gene
            )
            geneImpact.countImpactWithMutatedGeneWithContext(mutatedGeneWithContext, impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation)
        }

    }


    override fun validate(gene: Gene): Boolean = gene is OptionalGene

    override fun flatViewInnerImpact(): Map<String, Impact> {
        return mutableMapOf(
                "$id-activeImpact" to activeImpact
        ).plus(activeImpact.flatViewInnerImpact()).plus("$id-geneImpact" to geneImpact).plus(geneImpact.flatViewInnerImpact())
    }
}