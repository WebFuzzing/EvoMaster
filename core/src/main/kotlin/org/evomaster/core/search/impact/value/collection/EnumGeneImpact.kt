package org.evomaster.core.search.impact.value.collection

import org.evomaster.core.search.gene.EnumGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.impact.GeneImpact
import org.evomaster.core.search.impact.Impact
import org.evomaster.core.search.impact.MutatedGeneWithContext
import org.evomaster.core.search.impact.value.GeneralImpact

/**
 * created by manzh on 2019-09-09
 */
class EnumGeneImpact (
        id : String,
        degree: Double = 0.0,
        timesToManipulate : Int = 0,
        timesOfNoImpacts : Int = 0,
        conTimesOfNoImpacts : Int = 0,
        timesOfImpact : MutableMap<Int, Int> = mutableMapOf(),
        noImpactFromImpact : MutableMap<Int, Int> = mutableMapOf(),
        noImprovement : MutableMap<Int, Int> = mutableMapOf(),
        val values : List<GeneralImpact> = listOf()
)  : GeneImpact(
        id = id,
        degree = degree,
        timesToManipulate = timesToManipulate,
        timesOfNoImpacts = timesOfNoImpacts,
        conTimesOfNoImpacts = conTimesOfNoImpacts,
        timesOfImpact= timesOfImpact,
        noImpactFromImpact = noImpactFromImpact,
        noImprovement = noImprovement
) {

    constructor(id: String, gene: EnumGene<*>) : this (id, values = gene.values.mapIndexed { index, _ -> GeneralImpact(index.toString()) }.toList())

    override fun copy(): EnumGeneImpact {
        return EnumGeneImpact(id = id,
                degree = degree,
                timesToManipulate = timesToManipulate,
                timesOfNoImpacts = timesOfNoImpacts,
                conTimesOfNoImpacts = conTimesOfNoImpacts,
                timesOfImpact= timesOfImpact,
                noImpactFromImpact = noImpactFromImpact,
                noImprovement = noImprovement,
                values = values.map { it.copy() }
        )
    }

    override fun countImpactWithMutatedGeneWithContext(gc: MutatedGeneWithContext, impactTargets: Set<Int>, improvedTargets: Set<Int>) {
        countImpactAndPerformance(impactTargets = impactTargets, improvedTargets = improvedTargets)

        if (gc.current !is EnumGene<*>)
            throw IllegalStateException("gc.current (${gc.current::class.java.simpleName}) should be EnumGene")

        values[gc.current.index].countImpactAndPerformance(impactTargets = impactTargets, improvedTargets = improvedTargets)
    }

    override fun validate(gene: Gene): Boolean = gene is EnumGene<*>

    override fun flatViewInnerImpact(): Map<String, Impact> {
        return values.map { "$id-${it.id}" to it }.toMap()
    }

    override fun maxTimesOfNoImpact(): Int = values.size * 2
}