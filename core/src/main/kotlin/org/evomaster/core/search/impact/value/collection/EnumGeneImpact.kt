package org.evomaster.core.search.impact.value.collection

import org.evomaster.core.search.gene.EnumGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.impact.GeneImpact
import org.evomaster.core.search.impact.MutatedGeneWithContext
import org.evomaster.core.search.impact.value.GeneralImpact

/**
 * created by manzh on 2019-09-09
 */
class EnumGeneImpact (
        id: String,
        degree: Double = 0.0,
        timesToManipulate: Int = 0,
        timesOfImpact: Int = 0,
        timesOfNoImpacts: Int = 0,
        counter: Int = 0,
        positionSensitive: Boolean = false,
        val values : List<GeneralImpact> = listOf()
) : GeneImpact(id, degree, timesToManipulate, timesOfImpact, timesOfNoImpacts, counter, positionSensitive) {

    constructor(id: String, gene: EnumGene<*>) : this (id, values = gene.values.mapIndexed { index, _ -> GeneralImpact(index.toString()) }.toList())

    override fun copy(): EnumGeneImpact {
        return EnumGeneImpact(id, degree, timesToManipulate, timesOfImpact, timesOfNoImpacts, counter, positionSensitive, values)
    }

    override fun countImpactWithMutatedGeneWithContext(gc: MutatedGeneWithContext, hasImpact: Boolean, noImprovement: Boolean) {
        countImpactAndPerformance(hasImpact, noImprovement)

        if (gc.current !is EnumGene<*>)
            throw IllegalStateException("gc.current (${gc.current::class.java.simpleName}) should be EnumGene")

        values[gc.current.index].countImpactAndPerformance(hasImpact, noImprovement)
    }

    override fun validate(gene: Gene): Boolean = gene is EnumGene<*>
}