package org.evomaster.core.search.impact.impactInfoCollection.value.collection

import org.evomaster.core.search.gene.EnumGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.impact.impactInfoCollection.*

/**
 * created by manzh on 2019-09-09
 */
class EnumGeneImpact (sharedImpactInfo: SharedImpactInfo, specificImpactInfo: SpecificImpactInfo,
                      val values : List<Impact>
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
            values : List<Impact> = listOf()

    ) : this(
            SharedImpactInfo(id, degree, timesToManipulate, timesOfNoImpacts, timesOfNoImpactWithTargets, timesOfImpact),
            SpecificImpactInfo(noImpactFromImpact, noImprovement),
            values)

    constructor(id: String, gene: EnumGene<*>) : this (id, values = gene.values.mapIndexed { index, _ -> Impact(index.toString()) }.toList())

    override fun copy(): EnumGeneImpact {
        return EnumGeneImpact(
                shared.copy(),
                specific.copy(),
                values = values.map { it.copy() }
        )
    }

    override fun clone(): EnumGeneImpact {
        return EnumGeneImpact(
                shared.clone(),
                specific.clone(),
                values.map { it.clone() }
        )
    }

    override fun countImpactWithMutatedGeneWithContext(gc: MutatedGeneWithContext, noImpactTargets: Set<Int>, impactTargets: Set<Int>, improvedTargets: Set<Int>, onlyManipulation: Boolean) {
        countImpactAndPerformance(noImpactTargets = noImpactTargets, impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation)

        if (gc.current !is EnumGene<*>)
            throw IllegalStateException("gc.current (${gc.current::class.java.simpleName}) should be EnumGene")

        values[gc.current.index].countImpactAndPerformance(noImpactTargets = noImpactTargets, impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation)
    }

    override fun validate(gene: Gene): Boolean = gene is EnumGene<*>

    override fun flatViewInnerImpact(): Map<String, Impact> {
        return values.map { "${getId()}-${it.getId()}" to it }.toMap()
    }

    override fun maxTimesOfNoImpact(): Int = values.size * 2
}