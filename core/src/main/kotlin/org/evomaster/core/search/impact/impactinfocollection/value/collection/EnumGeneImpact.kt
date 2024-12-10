package org.evomaster.core.search.impact.impactinfocollection.value.collection

import org.evomaster.core.search.gene.collection.EnumGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.impact.impactinfocollection.*

/**
 * created by manzh on 2019-09-09
 */
class EnumGeneImpact (sharedImpactInfo: SharedImpactInfo, specificImpactInfo: SpecificImpactInfo,
                      val values : List<Impact>
) : GeneImpact(sharedImpactInfo, specificImpactInfo){

    constructor(id: String, gene: EnumGene<*>) : this (SharedImpactInfo(id), SpecificImpactInfo(), values = gene.values.mapIndexed { index, _ -> Impact(index.toString()) }.toList())

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
        countImpactAndPerformance(noImpactTargets = noImpactTargets, impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation, num = gc.numOfMutatedGene)

        if (gc.current !is EnumGene<*>)
            throw IllegalStateException("gc.current (${gc.current::class.java.simpleName}) should be EnumGene")

        values[gc.current.index].countImpactAndPerformance(noImpactTargets = noImpactTargets, impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation, num = 1)
    }

    override fun validate(gene: Gene): Boolean = gene is EnumGene<*>

    override fun flatViewInnerImpact(): Map<String, Impact> {
        return values.map { "${getId()}-${it.getId()}" to it }.toMap()
    }

    override fun maxTimesOfNoImpact(): Int = values.size * 2

    override fun innerImpacts(): List<Impact> {
        return values
    }
}