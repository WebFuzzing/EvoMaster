package org.evomaster.core.search.impact.value

import org.evomaster.core.search.gene.DisruptiveGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.impact.*

/**
 * created by manzh on 2019-09-09
 */
class DisruptiveGeneImpact (
        sharedImpactInfo: SharedImpactInfo,
        specificImpactInfo: SpecificImpactInfo,
        val geneImpact: GeneImpact
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
            geneImpact: GeneImpact

    ) : this(
            SharedImpactInfo(id, degree, timesToManipulate, timesOfNoImpacts, timesOfNoImpactWithTargets, timesOfImpact),
            SpecificImpactInfo(noImpactFromImpact, noImprovement),
            geneImpact
    )

    constructor(id : String, gene: DisruptiveGene<*>) : this(id, geneImpact = ImpactUtils.createGeneImpact(gene.gene, id))


    override fun copy(): DisruptiveGeneImpact {
        return DisruptiveGeneImpact(
                shared.copy(),
                specific.copy(),
                geneImpact = geneImpact.copy())
    }

    override fun clone(): DisruptiveGeneImpact {
        return DisruptiveGeneImpact(
                shared,
                specific.copy(),
                geneImpact = geneImpact.clone())
    }

    override fun validate(gene: Gene): Boolean = gene is DisruptiveGene<*>

    override fun countImpactWithMutatedGeneWithContext(gc: MutatedGeneWithContext, noImpactTargets : Set<Int>, impactTargets: Set<Int>, improvedTargets: Set<Int>, onlyManipulation: Boolean) {
        countImpactAndPerformance(noImpactTargets = noImpactTargets, impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation)

        if (gc.previous == null && impactTargets.isNotEmpty()) return
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
        geneImpact.countImpactWithMutatedGeneWithContext(mutatedGeneWithContext, noImpactTargets = noImpactTargets, impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation)
    }

    override fun flatViewInnerImpact(): Map<String, Impact> {
        return mutableMapOf(
                "${getId()}-geneImpact" to geneImpact
        ).plus(geneImpact.flatViewInnerImpact())
    }
}